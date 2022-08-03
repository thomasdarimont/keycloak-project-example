package com.github.thomasdarimont.keycloak.custom.auth.passwordform;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.PasswordForm;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Augments {@link PasswordForm} with additional handling of federated users.
 */
public class FederationAwarePasswordForm extends PasswordForm {

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {

        // TODO create keycloak issue for PasswordForm failing for federated users KEYCLOAK-XXX
        if (user.getFederationLink() != null) {
            // always allow password auth for federated users
            return true;
        }

        return super.configuredFor(session, realm, user);
    }

    @JBossLog
    @AutoService(AuthenticatorFactory.class)
    public static class Factory extends PasswordFormFactory {

        private static final FederationAwarePasswordForm INSTANCE = new FederationAwarePasswordForm();

        @Override
        public Authenticator create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            log.info("Overriding custom Keycloak PasswordFormFactory");
            super.postInit(factory);
        }
    }
}
