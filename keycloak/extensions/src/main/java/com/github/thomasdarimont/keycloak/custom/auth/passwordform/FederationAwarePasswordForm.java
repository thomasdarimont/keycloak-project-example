package com.github.thomasdarimont.keycloak.custom.auth.passwordform;

import org.keycloak.authentication.authenticators.browser.PasswordForm;
import org.keycloak.models.KeycloakSession;
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
}
