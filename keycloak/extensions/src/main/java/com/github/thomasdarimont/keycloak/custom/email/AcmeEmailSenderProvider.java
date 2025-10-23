package com.github.thomasdarimont.keycloak.custom.email;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.email.DefaultEmailAuthenticator;
import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.DefaultEmailSenderProviderFactory;
import org.keycloak.email.EmailAuthenticator;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.email.PasswordAuthEmailAuthenticator;
import org.keycloak.email.TokenAuthEmailAuthenticator;
import org.keycloak.models.KeycloakSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AcmeEmailSenderProvider extends DefaultEmailSenderProvider {

    private final KeycloakSession session;

    public AcmeEmailSenderProvider(KeycloakSession session, Map<EmailAuthenticator.AuthenticatorType, EmailAuthenticator> authenticators) {
        super(session, authenticators);
        this.session = session;
    }

    @Override
    public void send(Map<String, String> config, String address, String subject, String textBody, String htmlBody) throws EmailException {

        // adjust "from" via config object

        super.send(config, address, subject, textBody, htmlBody);
    }

//    @AutoService(EmailSenderProviderFactory.class)
    public static class Factory extends DefaultEmailSenderProviderFactory {

        private final Map<EmailAuthenticator.AuthenticatorType, EmailAuthenticator> emailAuthenticators = new ConcurrentHashMap<>();

        @Override
        public EmailSenderProvider create(KeycloakSession session) {
            return new AcmeEmailSenderProvider(session, emailAuthenticators);
        }

        @Override
        public void init(Config.Scope config) {
            emailAuthenticators.put(EmailAuthenticator.AuthenticatorType.NONE, new DefaultEmailAuthenticator());
            emailAuthenticators.put(EmailAuthenticator.AuthenticatorType.BASIC, new PasswordAuthEmailAuthenticator());
            emailAuthenticators.put(EmailAuthenticator.AuthenticatorType.TOKEN, new TokenAuthEmailAuthenticator());
        }

    }
}
