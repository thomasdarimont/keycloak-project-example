package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.auth;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceCookie;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials.TrustedDeviceCredentialModel;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials.TrustedDeviceCredentialProvider;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials.TrustedDeviceCredentialProviderFactory;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

@JBossLog
public class TrustedDeviceAuthenticator implements Authenticator, CredentialValidator<TrustedDeviceCredentialProvider> {

    static final String ID = "acme-auth-trusted-device";

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        UserModel user = context.getAuthenticationSession().getAuthenticatedUser();
        HttpRequest request = context.getHttpRequest();

        var receivedTrustedDeviceToken = TrustedDeviceCookie.parseDeviceTokenFromCookie(request, session);

        var credentialModel = TrustedDeviceCredentialModel.lookupTrustedDevice(session, realm, user, receivedTrustedDeviceToken);

        if (credentialModel == null) {
            log.debugf("Could not find trusted device device for user. realm=%s userId=%s", realm.getName(), user.getId());

            if (receivedTrustedDeviceToken != null) {
                // remove dangling invalid trusted device cookie
                TrustedDeviceCookie.removeDeviceCookie(session, realm);
            }

            context.attempted();
            return;
        }

        if (Time.currentTime() >= receivedTrustedDeviceToken.getExp()) {
            // token expired / remove existing credential
            boolean removed = session.userCredentialManager().removeStoredCredential(realm, user, credentialModel.getId());
            log.debugf("Detected expired trusted device. realm=%s userId=%s removed=%s", realm.getName(), user.getId(), removed);

            // remove dangling expired trusted device cookie
            TrustedDeviceCookie.removeDeviceCookie(session, realm);

            context.attempted();
            return;
        }

        // TODO invalidate trusted device if expired.
        // ManageTrustedDeviceAction.NUMBER_OF_DAYS_TO_TRUST_DEVICE

        log.info("Found trusted device.");
        context.getEvent().detail("trusted_device", "true");
        context.getEvent().detail("trusted_device_id", credentialModel.getDeviceId());
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // NOOP
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return session.userCredentialManager().isConfiguredFor(realm, user, TrustedDeviceCredentialModel.TYPE);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public TrustedDeviceCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (TrustedDeviceCredentialProvider) session.getProvider(CredentialProvider.class, TrustedDeviceCredentialProviderFactory.ID);
    }
}