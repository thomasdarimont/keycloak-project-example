package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceCookie;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceToken;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.action.ManageTrustedDeviceAction;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
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

@JBossLog
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

        UserCredentialManager ucm = session.userCredentialManager();
        CredentialModel credentialModel = ucm.getStoredCredentialById(realm, user, credentialId);

        boolean deleted = deleteMatchingDeviceCookieIfPresent(realm, credentialModel);
        if (deleted) {
            log.infof("Removed trusted device cookie for user. realm=%s userId=%s", realm.getName(), user.getId());
        }
        return ucm.removeStoredCredential(realm, user, credentialId);
    }

    /**
     * Try to delete device cookie if present
     *
     * @param realm
     * @param credentialModel
     * @return
     */
    private boolean deleteMatchingDeviceCookieIfPresent(RealmModel realm, CredentialModel credentialModel) {

        HttpRequest httpRequest = ResteasyProviderFactory.getContextData(HttpRequest.class);

        if (httpRequest == null) {
            return false;
        }

        TrustedDeviceToken trustedDeviceToken = TrustedDeviceCookie.parseDeviceTokenFromCookie(httpRequest, session);
        if (trustedDeviceToken == null || !trustedDeviceToken.getDeviceId().equals(credentialModel.getSecretData())) {
            return false;
        }

        // request comes from browser with device cookie that needs to be deleted
        TrustedDeviceCookie.removeDeviceCookie(session, realm);
        return true;
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
        // TODO make backup code removal configurable
        builder.removeable(true);
        builder.displayName("trusted-device-display-name");
        builder.helpText("trusted-device-help-text");

        // Note, that we can only have either a create or update action
        builder.updateAction(ManageTrustedDeviceAction.ID); // we use the update action to remove or "untrust" a device.
        //        builder.createAction(ManageTrustedDeviceAction.ID);

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

        if (!(credentialInput instanceof TrustedDeviceCredentialInput)) {
            return false;
        }

        TrustedDeviceCredentialInput tdci = (TrustedDeviceCredentialInput) credentialInput;

        CredentialModel credentialModel = session.userCredentialManager().getStoredCredentialsByTypeStream(realm, user, TrustedDeviceCredentialModel.TYPE)
                .filter(cm -> cm.getSecretData().equals(tdci.getChallengeResponse()))
                .findAny().orElse(null);

        return credentialModel != null;

    }
}
