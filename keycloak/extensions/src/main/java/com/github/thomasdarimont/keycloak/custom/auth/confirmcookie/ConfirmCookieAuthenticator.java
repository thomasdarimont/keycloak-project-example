package com.github.thomasdarimont.keycloak.custom.auth.confirmcookie;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.CookieAuthenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.List;
import java.util.Map;

@JBossLog
public class ConfirmCookieAuthenticator extends CookieAuthenticator {

    static final ConfirmCookieAuthenticator INSTANCE = new ConfirmCookieAuthenticator();

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(context.getSession(),
                context.getRealm(), true);
        if (authResult == null) {
            context.attempted();
            return;
        }

        Response response = context.form() //
                .createForm("login-confirm-cookie-form.ftl");
        context.challenge(response);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        super.authenticate(context);
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
            return "acme-auth-confirm-cookie";
        }

        @Override
        public Authenticator create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public String getDisplayType() {
            return "Acme: Confirm Cookie Authenticator";
        }

        @Override
        public String getHelpText() {
            return "Shows a form asking to confirm cookie";
        }

        @Override
        public String getReferenceCategory() {
            return "hello";
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
