package com.github.thomasdarimont.keycloak.custom.auth.backupcodes.credentials;

import com.github.thomasdarimont.keycloak.custom.auth.backupcodes.BackupCode;
import com.github.thomasdarimont.keycloak.custom.auth.backupcodes.BackupCodeConfig;
import com.github.thomasdarimont.keycloak.custom.auth.backupcodes.action.GenerateBackupCodeAction;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadata.CredentialTypeMetadataBuilder;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;

import java.util.List;
import java.util.stream.Collectors;

@Deprecated
@JBossLog
public class BackupCodeCredentialProvider implements CredentialProvider<CredentialModel>, CredentialInputValidator {

    public static final String ID = "custom-backup-code";

    private final KeycloakSession session;

    public BackupCodeCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return getType().equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return user.credentialManager().getStoredCredentialsByTypeStream(credentialType).findAny().isPresent();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {

        String codeInput = credentialInput.getChallengeResponse();

        BackupCodeConfig backupCodeConfig = getBackupCodeConfig(realm);
        PasswordHashProvider passwordHashProvider = session.getProvider(PasswordHashProvider.class,
                backupCodeConfig.getHashingProviderId());

        var cm = user.credentialManager();
        List<CredentialModel> backupCodes = cm.getStoredCredentialsByTypeStream(getType())
                .collect(Collectors.toList());

        for (CredentialModel backupCode : backupCodes) {
            // check if the given backup code matches
            if (passwordHashProvider.verify(codeInput, PasswordCredentialModel.createFromCredentialModel(backupCode))) {
                // we found matching backup code
                handleUsedBackupCode(cm, backupCode);
                return true;
            }
        }

        // no matching backup code found
        return false;
    }

    protected void handleUsedBackupCode(SubjectCredentialManager cm, CredentialModel backupCode) {
        // delete backup code entry
        cm.removeStoredCredentialById(backupCode.getId());
    }

    @Override
    public String getType() {
        return BackupCodeCredentialModel.TYPE;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel credentialModelInput) {

        if (!(credentialModelInput instanceof BackupCodeCredentialModel)) {
            return null;
        }

        BackupCodeConfig backupCodeConfig = getBackupCodeConfig(realm);

        BackupCodeCredentialModel backupCodeCredentialModel = (BackupCodeCredentialModel) credentialModelInput;
        BackupCode backupCode = backupCodeCredentialModel.getBackupCode();

        PasswordHashProvider passwordHashProvider = session.getProvider(PasswordHashProvider.class,
                backupCodeConfig.getHashingProviderId());
        if (passwordHashProvider == null) {
            log.errorf("Could not find hashProvider to hash backup codes. realm=%s user=%s providerId=%s",
                    realm.getId(), user.getId(), backupCodeConfig.getHashingProviderId());
            throw new RuntimeException("Cloud not find hashProvider to hash backup codes");
        }

        PasswordCredentialModel encodedBackupCode = encodeBackupCode(backupCode, backupCodeConfig, passwordHashProvider);
        CredentialModel backupCodeModel = createBackupCodeCredentialModel(backupCode, encodedBackupCode);

        user.credentialManager().createStoredCredential(backupCodeModel);

        return backupCodeModel;
    }

    protected CredentialModel createBackupCodeCredentialModel(BackupCode backupCode, PasswordCredentialModel encodedBackupCode) {

        CredentialModel model = new CredentialModel();
        model.setType(getType());
        model.setCreatedDate(backupCode.getCreatedAt());
        // TODO make userlabel configurable
        model.setUserLabel(createBackupCodeUserLabel(backupCode));
        model.setSecretData(encodedBackupCode.getSecretData());
        model.setCredentialData(encodedBackupCode.getCredentialData());
        return model;
    }

    protected PasswordCredentialModel encodeBackupCode(
            BackupCode backupCode, BackupCodeConfig backupCodeConfig, PasswordHashProvider passwordHashProvider) {
        return passwordHashProvider.encodedCredential(backupCode.getCode(), backupCodeConfig.getBackupCodeHashIterations());
    }

    protected String createBackupCodeUserLabel(BackupCode backupCode) {
        return "Backup-Code: " + backupCode.getId();
    }

    protected BackupCodeConfig getBackupCodeConfig(RealmModel realm) {
        return BackupCodeConfig.getConfig(realm);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return user.credentialManager().removeStoredCredentialById(credentialId);
    }

    @Override
    public CredentialModel getCredentialFromModel(CredentialModel model) {

        if (!getType().equals(model.getType())) {
            return null;
        }

        return model;
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {

        CredentialTypeMetadataBuilder builder = CredentialTypeMetadata.builder();
        builder.type(getType());
        builder.category(CredentialTypeMetadata.Category.TWO_FACTOR);
        builder.createAction(GenerateBackupCodeAction.ID);
        // TODO make backup code removal configurable
        builder.removeable(false);
        builder.displayName("backup-codes-display-name");
        builder.helpText("backup-codes-help-text");
        // builder.updateAction(GenerateBackupCodeAction.ID);
        // TODO configure proper FA icon for backup codes
        builder.iconCssClass("kcAuthenticatorBackupCodeClass");

        return builder.build(session);
    }

    @AutoService(CredentialProviderFactory.class)
    public static class Factory implements CredentialProviderFactory<BackupCodeCredentialProvider> {

        @Override
        public CredentialProvider<CredentialModel> create(KeycloakSession session) {
            return new BackupCodeCredentialProvider(session);
        }

        @Override
        public String getId() {
            return BackupCodeCredentialProvider.ID;
        }
    }
}
