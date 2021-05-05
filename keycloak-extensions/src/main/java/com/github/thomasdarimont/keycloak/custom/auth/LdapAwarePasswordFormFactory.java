package com.github.thomasdarimont.keycloak.custom.auth;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@JBossLog
@AutoService(AuthenticatorFactory.class)
public class LdapAwarePasswordFormFactory extends PasswordFormFactory {

    private static final LdapAwarePasswordForm INSTANCE = new LdapAwarePasswordForm();

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
