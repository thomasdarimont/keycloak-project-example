package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials;

import com.github.thomasdarimont.keycloak.custom.auth.backupcodes.BackupCode;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.action.ManageTrustedDeviceAction;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;

public class TrustedDeviceCredentialProvider implements CredentialProvider<CredentialModel>, CredentialInputValidator {

    private final KeycloakSession session;

    public TrustedDeviceCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getType() {
        return TrustedDeviceCredentialModel.TYPE;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel credentialModel) {

        CredentialModel trustedDeviceCredentialModel = createTrustedDeviceCredentialModel((TrustedDeviceCredentialModel) credentialModel);

        session.userCredentialManager().createCredential(realm, user, trustedDeviceCredentialModel);

        return trustedDeviceCredentialModel;
    }

    protected CredentialModel createTrustedDeviceCredentialModel(TrustedDeviceCredentialModel trustedDeviceCredentialModel) {

        CredentialModel model = new CredentialModel();
        model.setType(getType());
        model.setCreatedDate(Time.currentTimeMillis());
        // TODO make userlabel configurable
        model.setUserLabel(trustedDeviceCredentialModel.getUserLabel());
        model.setSecretData(trustedDeviceCredentialModel.getDeviceId());
        model.setCredentialData(null);
        return model;
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return session.userCredentialManager().removeStoredCredential(realm, user, credentialId);
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

        CredentialTypeMetadata.CredentialTypeMetadataBuilder builder = CredentialTypeMetadata.builder();
        builder.type(getType());
        builder.category(CredentialTypeMetadata.Category.TWO_FACTOR);
        builder.createAction(ManageTrustedDeviceAction.ID);
        // TODO make backup code removal configurable
        builder.removeable(true);
        builder.displayName("trusted-device-display-name");
        builder.helpText("trusted-device-help-text");
        // builder.updateAction(GenerateBackupCodeAction.ID);
        // TODO configure proper FA icon for backup codes
        builder.iconCssClass("kcAuthenticatorTrustedDeviceClass");

        return builder.build(session);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return TrustedDeviceCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return session.userCredentialManager().getStoredCredentialsByTypeStream(realm, user, credentialType).findAny().orElse(null) != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        return false;
    }
}
