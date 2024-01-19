package com.github.thomasdarimont.keycloak.custom.auth.hello;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.List;
import java.util.Map;

@JBossLog
public class HelloAuthenticator implements Authenticator {

    static final HelloAuthenticator INSTANCE = new HelloAuthenticator();

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // entrypoint
        // check auth
        // "force challenge if necessary"
        var authConfig = context.getAuthenticatorConfig();
        String message = authConfig == null ? "Hello" : authConfig.getConfig().getOrDefault("message", "Hello");
        String username = context.getAuthenticationSession().getAuthenticatedUser().getUsername();
        log.infof("%s %s%n", message, username);

        context.getEvent().detail("message", message);

        context.success();
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
            return "acme-auth-hello";
        }

        @Override
        public Authenticator create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public String getDisplayType() {
            return "Acme: Hello Authenticator";
        }

        @Override
        public String getHelpText() {
            return "Prints a greeting for the user to the console";
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
