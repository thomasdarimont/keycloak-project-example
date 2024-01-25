package com.github.thomasdarimont.keycloak.custom.auth.mfa.emailcode;

import com.github.thomasdarimont.keycloak.custom.account.AccountActivity;
import com.github.thomasdarimont.keycloak.custom.account.MfaChange;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

@JBossLog
public class EmailCodeCredentialProvider implements CredentialProvider<CredentialModel>, CredentialInputValidator {

    public static final String ID = "acme-mfa-email-code";

    private final KeycloakSession session;

    public EmailCodeCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return EmailCodeCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return user.credentialManager().getStoredCredentialsByTypeStream(credentialType).findAny().isPresent();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        return false;
    }

    @Override
    public String getType() {
        return EmailCodeCredentialModel.TYPE;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel credentialModel) {

        if (!(credentialModel instanceof EmailCodeCredentialModel)) {
            return null;
        }

        user.credentialManager().createStoredCredential(credentialModel);

        return credentialModel;
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        var credential = user.credentialManager().getStoredCredentialById(credentialId);
        var deleted = user.credentialManager().removeStoredCredentialById(credentialId);
        if (deleted) {
            AccountActivity.onUserMfaChanged(session, realm, user, credential, MfaChange.REMOVE);
        }
        return deleted;
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
        var builder = CredentialTypeMetadata.builder();
        builder.type(getType());
        builder.category(CredentialTypeMetadata.Category.TWO_FACTOR);
        builder.createAction(RegisterEmailCodeRequiredAction.ID);
        builder.removeable(true);
        builder.displayName("mfa-email-code-display-name");
        builder.helpText("mfa-email-code-help-text");
        // builder.updateAction(GenerateBackupCodeAction.ID);
        // TODO configure proper FA icon for email-code auth
        builder.iconCssClass("kcAuthenticatorMfaEmailCodeClass");
        return builder.build(session);
    }

    @SuppressWarnings("rawtypes")
    @AutoService(CredentialProviderFactory.class)
    public static class Factory implements CredentialProviderFactory<EmailCodeCredentialProvider> {

        @Override
        public CredentialProvider<CredentialModel> create(KeycloakSession session) {
            return new EmailCodeCredentialProvider(session);
        }

        @Override
        public String getId() {
            return EmailCodeCredentialProvider.ID;
        }
    }
}
