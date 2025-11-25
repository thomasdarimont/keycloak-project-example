package com.github.thomasdarimont.keycloak.custom.migration.acmecred;

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
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;

@JBossLog
public class AcmeCredentialProvider implements CredentialProvider<CredentialModel>, CredentialInputValidator {

    private final KeycloakSession session;

    public AcmeCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getType() {
        return AcmeCredentialModel.TYPE;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel credentialModel) {
        // we don't support acme-credential creation
        return null;
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return user.credentialManager().removeStoredCredentialById(credentialId);
    }

    @Override
    public CredentialModel getCredentialFromModel(CredentialModel model) {
        // we support the acme-password and normal password credential model
        // this is required to support the correct credential type in admin-console
        if (model.getType().equals(AcmeCredentialModel.TYPE)) {
            return AcmeCredentialModel.createFromCredentialModel(model);
        }
        return PasswordCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        CredentialTypeMetadata.CredentialTypeMetadataBuilder metadataBuilder = CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.BASIC_AUTHENTICATION)
                .displayName("Acme Password")
                .helpText("password-help-text")
                .iconCssClass("kcAuthenticatorPasswordClass");

        // Check if we are creating or updating password
        UserModel user = metadataContext.getUser();
        if (user != null && user.credentialManager().isConfiguredFor(getType())) {
            metadataBuilder.updateAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        } else {
            metadataBuilder.createAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        }

        return metadataBuilder
                .removeable(false)
                .build(session);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        // HACK: to support password input validation via UsernamePasswordForm authenticator
        // we need to pretend to accept credential type "password"
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        boolean supportedCredentialType = supportsCredentialType(credentialType);
        boolean acmeCredentialConfigured = isAcmeCredentialConfigured(user);
        return supportedCredentialType && acmeCredentialConfigured;
    }

    private boolean isAcmeCredentialConfigured(UserModel user) {
        return user.credentialManager().getCredentials().anyMatch(cm -> AcmeCredentialModel.TYPE.equals(cm.getType()));
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {

        CredentialModel credentialModel = user.credentialManager().getStoredCredentialsByTypeStream(AcmeCredentialModel.TYPE).findFirst().orElse(null);
        if (credentialModel == null) {
            // abort the acme password validation early
            return false;
        }

        String password = credentialInput.getChallengeResponse();

        AcmeCredentialModel acmeCredentialModel = AcmeCredentialModel.createFromCredentialModel(credentialModel);

        String algorithm = acmeCredentialModel.getAcmeCredentialData().getAlgorithm();
        boolean valid = switch (algorithm) {
            case "acme-sha1" -> {
                yield AcmePasswordValidator.validateLegacyPassword(password, acmeCredentialModel);
            }
            // add additional legacy password validations...
            default -> false;
        };

        if (valid) {
            migrateCredential(realm, user, password, acmeCredentialModel);
        }

        return valid;
    }

    protected void migrateCredential(RealmModel realm, UserModel user, String password, AcmeCredentialModel acmeCredentialModel) {

        // remove the old password
        user.credentialManager().removeStoredCredentialById(acmeCredentialModel.getId());

        // store the current password with the default hashing mechanism
        user.credentialManager().updateCredential(UserCredentialModel.password(password, false));

        // remove acme federation link
        // user.setFederationLink(null);

        log.infof("Migrated user password after successful acme-credential validation. realm=%s userId=%s username=%s", realm.getName(), user.getId(), user.getUsername());
    }

    @AutoService(CredentialProviderFactory.class)
    public static class Factory implements CredentialProviderFactory<AcmeCredentialProvider> {

        @Override
        public String getId() {
            return "acme";
        }

        @Override
        public AcmeCredentialProvider create(KeycloakSession session) {
            return new AcmeCredentialProvider(session);
        }
    }

}