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

    public static TrustedDeviceCredentialModel lookupTrustedDeviceCredentialModelFromCookie(KeycloakSession session, RealmModel realm, UserModel user, HttpRequest httpRequest) {

        if (user == null) {
            return null;
        }

        var trustedDeviceToken = TrustedDeviceCookie.parseDeviceTokenFromCookie(httpRequest, session);
        if (trustedDeviceToken == null) {
            return null;
        }

        if (Time.currentTime() >= trustedDeviceToken.getExp()) {
            // token expired
            return null;
        }

        var credentialModel = session.userCredentialManager().getStoredCredentialsByTypeStream(realm, user, TrustedDeviceCredentialModel.TYPE)
                .filter(cm -> cm.getSecretData().equals(trustedDeviceToken.getDeviceId()))
                .findAny().orElse(null);

        if (credentialModel == null) {
            return null;
        }

        return new TrustedDeviceCredentialModel(credentialModel.getId(), credentialModel.getUserLabel(), credentialModel.getSecretData());
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        var trustedDeviceCredentialModel = lookupTrustedDeviceCredentialModelFromCookie( //
                context.getSession(), //
                context.getRealm(),  //
                context.getAuthenticationSession().getAuthenticatedUser(), //
                context.getHttpRequest() //
        );

        if (trustedDeviceCredentialModel == null) {
            log.info("Unknown device detected!");
            context.attempted();
            return;
        }

        log.info("Found trusted device.");
        context.getEvent().detail("trusted_device", "true");
        context.getEvent().detail("trusted_device_id", trustedDeviceCredentialModel.getDeviceId());
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
        return (TrustedDeviceCredentialProvider)session.getProvider(CredentialProvider.class, TrustedDeviceCredentialProviderFactory.ID);
    }
}
