package com.github.thomasdarimont.keycloak.custom.auth.customcookie;

import com.google.auto.service.AutoService;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.CookieAuthenticator;
import org.keycloak.authentication.authenticators.browser.CookieAuthenticatorFactory;
import org.keycloak.models.KeycloakSession;

public class CustomCookieAuthenticator extends CookieAuthenticator {

    private final KeycloakSession session;

    public CustomCookieAuthenticator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        super.authenticate(context);
    }

    // @AutoService(AuthenticatorFactory.class)
    public static class Factory extends CookieAuthenticatorFactory {
        @Override
        public Authenticator create(KeycloakSession session) {
            return new CustomCookieAuthenticator(session);
        }
    }
}
