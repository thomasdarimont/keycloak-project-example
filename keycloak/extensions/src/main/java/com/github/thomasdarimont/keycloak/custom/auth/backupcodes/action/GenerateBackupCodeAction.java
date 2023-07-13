package com.github.thomasdarimont.keycloak.custom.auth.backupcodes.action;

import com.github.thomasdarimont.keycloak.custom.auth.backupcodes.BackupCode;
import com.github.thomasdarimont.keycloak.custom.auth.backupcodes.BackupCodeConfig;
import com.github.thomasdarimont.keycloak.custom.auth.backupcodes.BackupCodeGenerator;
import com.github.thomasdarimont.keycloak.custom.auth.backupcodes.credentials.BackupCodeCredentialModel;
import com.github.thomasdarimont.keycloak.custom.auth.backupcodes.credentials.BackupCodeCredentialProvider;
import com.github.thomasdarimont.keycloak.custom.support.RequiredActionUtils;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.RealmBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;

import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Deprecated
@JBossLog
public class GenerateBackupCodeAction implements RequiredActionProvider {

    public static final String ID = "acme-generate-backup-codes";

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        // NOOP
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        context.challenge(generateBackupCodesForm(context));
    }

    protected Response generateBackupCodesForm(RequiredActionContext context) {
        return createBackupCodesForm(context).createForm("backup-codes.ftl");
    }

    protected LoginFormsProvider createBackupCodesForm(RequiredActionContext context) {

        UserModel user = context.getAuthenticationSession().getAuthenticatedUser();
        String username = user.getUsername();

        LoginFormsProvider form = context.form();
        form.setAttribute("username", username);
        // hint for the form that the current user already has backup codes that will be overridden by new codes.
        form.setAttribute("backupCodesPresent", backupCodesConfiguredForUser(context, user));
        return form;
    }

    protected boolean backupCodesConfiguredForUser(RequiredActionContext context, UserModel user) {
        return user.credentialManager().getStoredCredentialsByTypeStream(getBackupCodeType())
                .findAny().isPresent();
    }

    protected String getBackupCodeType() {
        return BackupCodeCredentialModel.TYPE;
    }

    protected List<BackupCode> createNewBackupCodes(RealmModel realm, UserModel user, KeycloakSession session) {

        BackupCodeConfig backupCodeConfig = getBackupCodeConfig(realm);

        List<BackupCode> backupCodes = new ArrayList<>();
        long now = Time.currentTimeMillis();
        var credentialProvider = session.getProvider(BackupCodeCredentialProvider.class, BackupCodeCredentialProvider.ID);
        for (int i = 1, count = backupCodeConfig.getBackupCodeCount(); i <= count; i++) {
            BackupCode backupCode = generateBackupCode(backupCodeConfig, now, i);
            try {
                // create and store new backup-code credential model

                credentialProvider.createCredential(realm, user, new BackupCodeCredentialModel(backupCode));
                backupCodes.add(backupCode);
            } catch (Exception ex) {
                log.warnf(ex, "Cloud not create backup code for user. realm=%s user=%s", realm.getId(), user.getId());
            }
        }
        return backupCodes;
    }

    protected BackupCode generateBackupCode(BackupCodeConfig backupCodeConfig, long timestamp, int backupCodeIndex) {
        String code = BackupCodeGenerator.generateCode(backupCodeConfig.getBackupCodeLength());
        return new BackupCode("" + backupCodeIndex, code, timestamp);
    }

    protected BackupCodeConfig getBackupCodeConfig(RealmModel realm) {
        return BackupCodeConfig.getConfig(realm);
    }

    @Override
    public void processAction(RequiredActionContext context) {

        if (RequiredActionUtils.isCancelApplicationInitiatedAction(context)) {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            AuthenticationManager.setKcActionStatus(GenerateBackupCodeAction.ID, RequiredActionContext.KcActionStatus.CANCELLED, authSession);
            context.success();
            return;
        }

        EventBuilder event = context.getEvent();
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();

        removeExistingBackupCodesIfPresent(realm, user, session);

        // TODO introduce dedicated UPDATE_BACKUP_CODE or UPDATE_SECOND_FACTOR event
        List<BackupCode> backupCodes = createNewBackupCodes(realm, user, session);

        // remove required action
        context.getUser().removeRequiredAction(ID);
        context.success();

        event.event(EventType.CUSTOM_REQUIRED_ACTION);
        event.detail("action_id", ID);
        event.detail("backup_code", "true");
        event.success();

        // Show backup code download form
        context.challenge(createDownloadForm(context, backupCodes).createForm("backup-codes-download.ftl"));
    }

    protected void removeExistingBackupCodesIfPresent(RealmModel realm, UserModel user, KeycloakSession session) {

        var credentialManager = user.credentialManager();
        log.debugf("Removing existing backup codes. realm=%s user=%s", realm.getId(), user.getId());
        List<CredentialModel> credentials = credentialManager.getStoredCredentialsByTypeStream(getBackupCodeType())
                .collect(Collectors.toList());
        for (CredentialModel credential : credentials) {
            credentialManager.removeStoredCredentialById(credential.getId());
        }
        log.debugf("Removed existing backup codes. realm=%s user=%s", realm.getId(), user.getId());
    }

    protected LoginFormsProvider createDownloadForm(RequiredActionContext context, List<BackupCode> backupCodes) {

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        UserModel user = authSession.getAuthenticatedUser();
        ZonedDateTime createdAt = LocalDateTime.now().atZone(ZoneId.systemDefault());

        LoginFormsProvider form = context.form();
        form.setAttribute("username", user.getUsername());
        form.setAttribute("createdAt", createdAt.toInstant().toEpochMilli());

        Locale locale = context.getSession().getContext().resolveLocale(user);
        String createdAtDate = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).localizedBy(locale).format(createdAt);
        form.setAttribute("createdAtDate", createdAtDate);

        form.setAttribute("realm", new RealmBean(context.getRealm()));
        form.setAttribute("backupCodes", backupCodes);
        return form;
    }

    @Override
    public void close() {
        // NOOP
    }

    @AutoService(RequiredActionFactory.class)
    public static class Factory implements RequiredActionFactory {

        public static final GenerateBackupCodeAction INSTANCE = new GenerateBackupCodeAction();

        @Override
        public RequiredActionProvider create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public void init(Config.Scope config) {
            // NOOP
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            // NOOP
        }

        @Override
        public void close() {
            // NOOP
        }

        @Override
        public String getId() {
            return GenerateBackupCodeAction.ID;
        }

        @Override
        public String getDisplayText() {
            return "Acme: Generate Backup Codes";
        }

        @Override
        public boolean isOneTimeAction() {
            return true;
        }
    }

}
