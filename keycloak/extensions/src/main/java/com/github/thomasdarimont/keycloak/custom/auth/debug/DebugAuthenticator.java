package com.github.thomasdarimont.keycloak.custom.auth.debug;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@JBossLog
public class DebugAuthenticator implements Authenticator {

    public final static String DEBUG_MESSAGE_TEMPLATE_KEY = "debugMessageTemplate";
    public static final String DEFAULT_DEBUG_MESSAGE = "{alias} User{userId={userId}, username={username}, email={email}} Client{clientId={clientId}, clientUuid={clientUuid}}";

    public DebugAuthenticator() {
    }

    @Override
    public void authenticate(AuthenticationFlowContext authenticationFlowContext) {

        String debugMessage = DEFAULT_DEBUG_MESSAGE;

        var authenticatorConfig = authenticationFlowContext.getAuthenticatorConfig();
        if (authenticatorConfig != null && authenticatorConfig.getConfig() != null) {
            debugMessage = authenticatorConfig.getConfig().getOrDefault(DEBUG_MESSAGE_TEMPLATE_KEY, DEFAULT_DEBUG_MESSAGE);
            String alias = authenticatorConfig.getAlias();
            if (alias == null) {
                alias = "";
            }
            debugMessage = debugMessage.replace("{alias}", alias);
        }

        // authenticationFlowContext.getExecution();
        // Post Broker Login after First Broker Login
        var authenticationSession = authenticationFlowContext.getAuthenticationSession();
        String postBrokerLoginAfterFirstBrokerLogin = authenticationSession.getAuthNote(PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);
        if (postBrokerLoginAfterFirstBrokerLogin != null) {
            debugMessage += " postBrokerLoginAfterFirstBrokerLogin=true";
        }

        // Post Broker Login after consecutive login
        String postBrokerLoginAfterConsecutiveLogin = authenticationSession.getAuthNote(PostBrokerLoginConstants.PBL_AFTER_FIRST_BROKER_LOGIN);
        if (postBrokerLoginAfterConsecutiveLogin != null) {
            debugMessage += " postBrokerLoginAfterConsecutiveLogin=true";
        }

        var user = authenticationFlowContext.getUser();
        if (user != null) {
            debugMessage = debugMessage.replaceAll(Pattern.quote("{userId}"), user.getId());
            debugMessage = debugMessage.replaceAll(Pattern.quote("{username}"), user.getUsername());
            debugMessage = debugMessage.replaceAll(Pattern.quote("{email}"), user.getEmail());
        }
        var client = authenticationSession.getClient();
        if (client != null) {
            debugMessage = debugMessage.replaceAll(Pattern.quote("{clientUuid}"), client.getClientId());
            debugMessage = debugMessage.replaceAll(Pattern.quote("{clientId}"), client.getClientId());
        }

        log.debug(debugMessage);

        authenticationFlowContext.success();
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {

    }

    @Override
    public void close() {

    }

    @AutoService(AuthenticatorFactory.class)
    public static class Factory implements AuthenticatorFactory {

        private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

        static {
            var list = ProviderConfigurationBuilder.create() //

                    .property().name(DEBUG_MESSAGE_TEMPLATE_KEY) //
                    .type(ProviderConfigProperty.STRING_TYPE) //
                    .label("Debug Message") //
                    .defaultValue(DEFAULT_DEBUG_MESSAGE) //
                    .helpText("Debug Message template. Supported Parameters: {username}, {email}, {userId}, {clientId}") //
                    .add() //

                    .build();

            CONFIG_PROPERTIES = Collections.unmodifiableList(list);
        }

        @Override
        public String getId() {
            return "acme-debug-auth";
        }

        @Override
        public Authenticator create(KeycloakSession session) {
            return new DebugAuthenticator();
        }


        @Override
        public String getDisplayType() {
            return "Acme: Debug Auth Step";
        }

        @Override
        public String getHelpText() {
            return "Prints the current step to the console.";
        }

        @Override
        public String getReferenceCategory() {
            return null;
        }

        @Override
        public boolean isConfigurable() {
            return true;
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
        public List<ProviderConfigProperty> getConfigProperties() {
            return CONFIG_PROPERTIES;
        }

        @Override
        public void init(Config.Scope scope) {
            // NOOP
        }

        @Override
        public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
            // NOOP
        }

        @Override
        public void close() {
            // NOOP
        }
    }

}
