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

    public static TrustedDeviceCredentialModel lookupTrustedDevice(KeycloakSession session, RealmModel realm, UserModel user, HttpRequest httpRequest) {

        if (user == null) {
            return null;
        }

        DeviceToken deviceToken = DeviceCookie.parseDeviceTokenFromCookie(httpRequest, session);
        if (deviceToken == null) {
            return null;
        }


        CredentialModel credentialModel = session.userCredentialManager().getStoredCredentialsByTypeStream(realm, user, TrustedDeviceCredentialModel.TYPE)
                .filter(cm -> cm.getSecretData().equals(deviceToken.getDeviceId()))
                .findAny().orElse(null);

        if (credentialModel == null) {
            return null;
        }

        return new TrustedDeviceCredentialModel(credentialModel.getId(), credentialModel.getUserLabel(), credentialModel.getSecretData());
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        TrustedDeviceCredentialModel candidate = lookupTrustedDevice( //
                context.getSession(), //
                context.getRealm(),  //
                context.getAuthenticationSession().getAuthenticatedUser(), //
                context.getHttpRequest() //
        );

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
