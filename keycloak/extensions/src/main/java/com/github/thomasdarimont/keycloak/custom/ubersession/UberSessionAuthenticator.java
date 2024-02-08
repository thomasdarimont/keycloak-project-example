package com.github.thomasdarimont.keycloak.custom.ubersession;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.List;
import java.util.Map;

@Slf4j
public class UberSessionAuthenticator implements Authenticator {

    static final UberSessionAuthenticator INSTANCE = new UberSessionAuthenticator();

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(session, realm, true);
        if (authResult != null) {
            context.attempted();
            return;
        }

        Cookie cookie = UberSessionCookie.readCookie(session);
        if (cookie == null|| "".equals(cookie.getValue())) {
            context.attempted();
            return;
        }

        cookie.getValue();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // handle user input
        // check input
        // mark as success
        // or on failure -> force challenge again
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }

    @AutoService(AuthenticatorFactory.class)
    public static class Factory implements AuthenticatorFactory, ServerInfoAwareProviderFactory {

        @Override
        public String getId() {
            return "acme-auth-ubersession";
        }

        @Override
        public Authenticator create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public String getDisplayType() {
            return "Acme: Uber Session Authenticator";
        }

        @Override
        public String getHelpText() {
            return "Creates a long lived cookie";
        }

        @Override
        public String getReferenceCategory() {
            return "cookie";
        }

        @Override
        public boolean isConfigurable() {
            return true;
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {

            List<ProviderConfigProperty> properties = ProviderConfigurationBuilder.create() //
                    .property().name("message").label("Message")
                    .helpText("Message text").type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("hello").add()
                    .build();

            return properties;
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
        public void postInit(KeycloakSessionFactory factory) {
            // called after factory is found
        }

        @Override
        public void init(Config.Scope config) {

            // spi-authenticator-acme-auth-hello-message
//            config.get("message");
            // called when provider factory is used
        }


        @Override
        public void close() {

        }

        @Override
        public Map<String, String> getOperationalInfo() {
            return Map.of("info", "infoValue");
        }
    }
}

