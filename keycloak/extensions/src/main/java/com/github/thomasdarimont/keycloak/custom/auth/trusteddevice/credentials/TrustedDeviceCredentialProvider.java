package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceCookie;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceToken;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.action.ManageTrustedDeviceAction;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.common.util.Resteasy;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;

@JBossLog
public class TrustedDeviceCredentialProvider implements CredentialProvider<CredentialModel>, CredentialInputValidator {

    public static final String ID = "custom-trusted-device";

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

        var cm = user.credentialManager();
        var storedCredential = cm.createStoredCredential(trustedDeviceCredentialModel);

        // The execution order of the credential backed authenticators is controlled by the order of the stored credentials
        // not only by the order of the authenticator. There fore, we need to move the new device-credential right after the password credential.
        cm.getStoredCredentialsByTypeStream(PasswordCredentialModel.TYPE)
                .findFirst()
                .ifPresent(passwordModel ->
                        cm.moveStoredCredentialTo(storedCredential.getId(), passwordModel.getId()));


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

        var cm = user.credentialManager();
        var credentialModel = cm.getStoredCredentialById(credentialId);

        boolean deleted = deleteMatchingDeviceCookieIfPresent(realm, credentialModel);
        if (deleted) {
            log.infof("Removed trusted device cookie for user. realm=%s userId=%s", realm.getName(), user.getId());
        }
        return cm.removeStoredCredentialById(credentialId);
    }

    /**
     * Try to delete device cookie if present
     *
     * @param realm
     * @param credentialModel
     * @return
     */
    private boolean deleteMatchingDeviceCookieIfPresent(RealmModel realm, CredentialModel credentialModel) {

        var httpRequest = Resteasy.getContextData(HttpRequest.class);

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

        var builder = CredentialTypeMetadata.builder();
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
        return user.credentialManager().getStoredCredentialsByTypeStream(credentialType).findAny().orElse(null) != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {

        if (!(credentialInput instanceof TrustedDeviceCredentialInput)) {
            return false;
        }

        var tdci = (TrustedDeviceCredentialInput) credentialInput;
        var deviceId = tdci.getChallengeResponse();

        var credentialModel = user.credentialManager().getStoredCredentialsByTypeStream(TrustedDeviceCredentialModel.TYPE)
                .filter(cm -> cm.getSecretData().equals(deviceId))
                .findAny().orElse(null);

        return credentialModel != null;
    }

    @SuppressWarnings("rawtypes")
    @AutoService(CredentialProviderFactory.class)
    public static class Factory implements CredentialProviderFactory<TrustedDeviceCredentialProvider> {

        @Override
        public CredentialProvider<CredentialModel> create(KeycloakSession session) {
            return new TrustedDeviceCredentialProvider(session);
        }

        @Override
        public String getId() {
            return TrustedDeviceCredentialProvider.ID;
        }
    }
}