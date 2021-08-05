package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.auth;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.DeviceCookie;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.DeviceToken;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials.TrustedDeviceCredentialModel;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

@JBossLog
public class TrustedDeviceAuthenticator implements Authenticator {

    static final String ID = "acme-auth-trusted-device";

    public static TrustedDeviceCredentialModel lookupTrustedDevice(AuthenticationFlowContext context) {
        HttpRequest httpRequest = context.getHttpRequest();
        KeycloakSession session = context.getSession();
        UserModel user = context.getAuthenticationSession().getAuthenticatedUser();
        if (user == null) {
            return null;
        }

        DeviceToken deviceToken = DeviceCookie.parseDeviceTokenFromCookie(httpRequest, session);
        if (deviceToken == null) {
            return null;
        }

        RealmModel realm = context.getRealm();
        CredentialModel credentialModel = session.userCredentialManager().getStoredCredentialsByTypeStream(realm, context.getUser(), TrustedDeviceCredentialModel.TYPE)
                .filter(cm -> cm.getSecretData().equals(deviceToken.getDeviceId()))
                .findAny().orElse(null);

        if (credentialModel == null) {
            return null;
        }

        return new TrustedDeviceCredentialModel(credentialModel.getUserLabel(), credentialModel.getSecretData());
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        TrustedDeviceCredentialModel candidate = lookupTrustedDevice(context);

        if (candidate == null) {
            log.info("Unknown device detected!");
            context.attempted();
            return;
        }

        log.info("Found trusted device.");
        context.getEvent().detail("trusted_device", "true");
        context.getEvent().detail("trusted_device_id", candidate.getDeviceId());
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
}
