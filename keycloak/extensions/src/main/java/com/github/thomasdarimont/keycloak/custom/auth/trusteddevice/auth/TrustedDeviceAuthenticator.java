package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.auth;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceCookie;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials.TrustedDeviceCredentialModel;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials.TrustedDeviceCredentialProvider;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

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

        var credentialModel = user.credentialManager().getStoredCredentialsByTypeStream(TrustedDeviceCredentialModel.TYPE).filter(cm -> cm.getSecretData().equals(trustedDeviceToken.getDeviceId())).findAny().orElse(null);

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
        return user.credentialManager().isConfiguredFor(TrustedDeviceCredentialModel.TYPE);
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
        return (TrustedDeviceCredentialProvider) session.getProvider(CredentialProvider.class, TrustedDeviceCredentialProvider.ID);
    }

    @AutoService(AuthenticatorFactory.class)
    public static class Factory implements AuthenticatorFactory {

        private static final TrustedDeviceAuthenticator INSTANCE = new TrustedDeviceAuthenticator();

        @Override
        public String getId() {
            return TrustedDeviceAuthenticator.ID;
        }

        @Override
        public String getDisplayType() {
            return "Acme: Trusted Device Authenticator";
        }

        @Override
        public String getHelpText() {
            return "Trusted Device to suppress MFA";
        }

        @Override
        public boolean isConfigurable() {
            return false;
        }

        @Override
        public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
            return REQUIREMENT_CHOICES;
        }

        @Override
        public boolean isUserSetupAllowed() {
            return false;
        }

        @Override
        public String getReferenceCategory() {
            return TrustedDeviceCredentialModel.TYPE;
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            return Collections.emptyList();
        }

        @Override
        public Authenticator create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public void init(Config.Scope config) {
            // NOOP
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            // NOOP
        }

        @Override
        public void close() {
            // NOOP
        }
    }
}
