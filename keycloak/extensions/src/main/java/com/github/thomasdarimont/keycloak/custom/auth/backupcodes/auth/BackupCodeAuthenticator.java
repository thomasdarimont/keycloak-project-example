package com.github.thomasdarimont.keycloak.custom.auth.backupcodes.auth;

import com.github.thomasdarimont.keycloak.custom.auth.backupcodes.action.GenerateBackupCodeAction;
import com.github.thomasdarimont.keycloak.custom.auth.backupcodes.credentials.BackupCodeCredentialModel;
import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.credentials.SmsCredentialModel;
import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.AbstractFormAuthenticator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.keycloak.authentication.authenticators.util.AuthenticatorUtils.getDisabledByBruteForceEventError;
import static org.keycloak.services.validation.Validation.FIELD_USERNAME;

@Deprecated
public class BackupCodeAuthenticator extends AbstractFormAuthenticator {

    public static final String ID = "acme-auth-backup-code";

    public static final String FIELD_BACKUP_CODE = "backupCode";

    public static final String MESSAGE_BACKUP_CODE_INVALID = "backup-code-invalid";

    public static final String CONFIG_RENEW_BACKUP_CODES_ON_EXHAUSTION = "renew-backup-codes-on-exhaustion";

    public static final String DEFAULT_RENEW_BACKUP_CODES_ON_EXHAUSTION = "true";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        Response challengeResponse = context.form().createForm("login-backup-codes.ftl");
        context.challenge(challengeResponse);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.cancelLogin();
            return;
        }
        if (validateBackupCode(context, context.getUser(), formData)) {
            context.success();
        }
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {

        // TODO revise handling of backup code auth prompt -> should we always ask for backup codes if present and no other 2FA is configured?
        if (isSecondFactorRequired(session, realm, user) && !isSecondFactorConfigured(session, realm, user)) {
            // we only allow checking for backup codes if another MFA is registered
            return false;
        }

        return user.credentialManager().isConfiguredFor(BackupCodeCredentialModel.TYPE);
    }

    protected boolean isSecondFactorRequired(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    protected boolean isSecondFactorConfigured(KeycloakSession session, RealmModel realm, UserModel user) {
        var cm = user.credentialManager();
        return cm.isConfiguredFor(OTPCredentialModel.TYPE) || cm.isConfiguredFor(SmsCredentialModel.TYPE);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction(GenerateBackupCodeAction.ID);
    }

    public boolean validateBackupCode(AuthenticationFlowContext context, UserModel user, MultivaluedMap<String, String> inputData) {

        String backupCodeInput = inputData.getFirst(FIELD_BACKUP_CODE);
        if (backupCodeInput == null || backupCodeInput.isEmpty()) {
            return badBackupCodeHandler(context, user, true);
        }

        // note backup_code usage in event
        context.getEvent().detail("backup_code", "true");

        if (isDisabledByBruteForce(context, user)) {
            return false;
        }

        UserCredentialModel backupCode = new UserCredentialModel(null, BackupCodeCredentialModel.TYPE, backupCodeInput, false);

        boolean backupCodeValid = user.credentialManager().isValid(backupCode);
        if (!backupCodeValid) {
            return badBackupCodeHandler(context, user, false);
        }

        checkForRemainingBackupCodes(context, user);

        return true;
    }

    protected void checkForRemainingBackupCodes(AuthenticationFlowContext context, UserModel user) {

        // check if there are remaining backup-codes left, otherwise add required action to user
        boolean remainingBackupCodesPresent = user.credentialManager().isConfiguredFor(BackupCodeCredentialModel.TYPE);
        if (remainingBackupCodesPresent) {
            return;
        }

        boolean renewBackupCodesOnExhaustion =
                Boolean.parseBoolean(getConfig(context, CONFIG_RENEW_BACKUP_CODES_ON_EXHAUSTION, DEFAULT_RENEW_BACKUP_CODES_ON_EXHAUSTION));
        if (renewBackupCodesOnExhaustion) {
            user.addRequiredAction(GenerateBackupCodeAction.ID);
        }
    }

    protected String getConfig(AuthenticationFlowContext context, String key, String defaultValue) {

        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        if (configModel == null) {
            return defaultValue;
        }

        Map<String, String> config = configModel.getConfig();
        if (config == null) {
            return defaultValue;
        }

        return config.getOrDefault(key, defaultValue);
    }

    protected boolean isDisabledByBruteForce(AuthenticationFlowContext context, UserModel user) {

        String bruteForceError = getDisabledByBruteForceEventError(context.getProtector(), context.getSession(), context.getRealm(), user);
        if (bruteForceError == null) {
            return false;
        }

        context.getEvent().user(user);
        context.getEvent().error(bruteForceError);
        Response challengeResponse = challenge(context, disabledByBruteForceError(), disabledByBruteForceFieldError());
        context.forceChallenge(challengeResponse);
        return true;
    }

    protected Response challenge(AuthenticationFlowContext context, String error, String field) {
        return createLoginForm(context, error, field).createForm("login-backup-codes.ftl");
    }

    protected LoginFormsProvider createLoginForm(AuthenticationFlowContext context, String error, String field) {

        LoginFormsProvider form = context.form()
                .setExecution(context.getExecution().getId());
        if (error == null) {
            return form;
        }

        if (field != null) {
            form.addError(new FormMessage(field, error));
        } else {
            form.setError(error);
        }
        return form;
    }

    protected boolean badBackupCodeHandler(AuthenticationFlowContext context, UserModel user, boolean emptyBackupCode) {

        EventBuilder event = context.getEvent();

        event.user(user);
        event.error(Errors.INVALID_USER_CREDENTIALS);

        Response challengeResponse = challenge(context, MESSAGE_BACKUP_CODE_INVALID, FIELD_BACKUP_CODE);
        if (emptyBackupCode) {
            context.forceChallenge(challengeResponse);
        } else {
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
        }
        return false;
    }

    protected String disabledByBruteForceError() {
        return Messages.INVALID_USER;
    }

    protected String disabledByBruteForceFieldError() {
        return FIELD_USERNAME;
    }

    @AutoService(AuthenticatorFactory.class)
    public static class Factory implements AuthenticatorFactory {

        private static final BackupCodeAuthenticator INSTANCE = new BackupCodeAuthenticator();

        private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

        static {
            List<ProviderConfigProperty> list = ProviderConfigurationBuilder
                    .create()
// TODO figure out how to access provider configuration in isConfiguredFor
//                .property().name("secondFactorRequired")
//                .type(ProviderConfigProperty.STRING_TYPE)
//                .label("Required Second Factor Credential Type")
//                .defaultValue(OTPCredentialModel.TYPE)
//                .helpText("If the credential model type is configured for the user the authenticator is offered." +
//                        "If the value is empty the authenticator is always offered.")
//                .add()
                    .build();

            CONFIG_PROPERTIES = Collections.unmodifiableList(list);
        }

        @Override
        public String getId() {
            return BackupCodeAuthenticator.ID;
        }

        @Override
        public String getDisplayType() {
            return "Acme: Backup Code Authenticator";
        }

        @Override
        public String getHelpText() {
            return "Backup Codes for 2FA Recovery";
        }

        @Override
        public boolean isConfigurable() {
            return false;
        }

        @Override
        public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
            return REQUIREMENT_CHOICES;
        }

        @Override
        public boolean isUserSetupAllowed() {
            return false;
        }

        @Override
        public String getReferenceCategory() {
            return BackupCodeCredentialModel.TYPE;
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            return CONFIG_PROPERTIES;
        }

        @Override
        public Authenticator create(KeycloakSession session) {
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
    }
}
