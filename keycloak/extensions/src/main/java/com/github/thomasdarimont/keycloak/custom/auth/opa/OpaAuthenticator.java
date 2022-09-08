package com.github.thomasdarimont.keycloak.custom.auth.opa;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

@JBossLog
public class OpaAuthenticator implements Authenticator {

    private final KeycloakSession session;
    private final OpaClient opaClient;

    public OpaAuthenticator(KeycloakSession session, OpaClient opaClient) {
        this.session = session;
        this.opaClient = opaClient;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        var realm = context.getRealm();
        var user = context.getUser();
        var authSession = context.getAuthenticationSession();

        var access = opaClient.checkAccess(session, context.getAuthenticatorConfig(), realm, user, authSession.getClient(), OpaClient.OPA_ACTION_LOGIN);

        if (!access.isAllowed()) {
            var loginForm = session.getProvider(LoginFormsProvider.class);
            String hint = access.getHint();
            if (hint == null) {
                hint = Messages.ACCESS_DENIED;
            }
            loginForm.setError(hint, user.getUsername());
            context.failure(AuthenticationFlowError.ACCESS_DENIED, loginForm.createErrorPage(Response.Status.FORBIDDEN));
            return;
        }

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
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    @AutoService(AuthenticatorFactory.class)
    public static class OpaAuthenticatorFactory implements AuthenticatorFactory {


        private OpaClient opaClient;

        private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

        static {
            var list = ProviderConfigurationBuilder.create() //

                    .property().name(OpaClient.OPA_AUTHZ_URL) //
                    .type(ProviderConfigProperty.STRING_TYPE) //
                    .label("Authz Server URL") //
                    .defaultValue(OpaClient.DEFAULT_OPA_AUTHZ_URL) //
                    .helpText("URL of OPA Authz Server Resource") //
                    .add() //

                    .property().name(OpaClient.OPA_USE_USER_ATTRIBUTES) //
                    .type(ProviderConfigProperty.BOOLEAN_TYPE) //
                    .label("Use user attributes") //
                    .defaultValue("true") //
                    .helpText("If enabled, user attributes will be sent with authz requests.") //
                    .add() //

                    .property().name(OpaClient.OPA_USER_ATTRIBUTES) //
                    .type(ProviderConfigProperty.STRING_TYPE) //
                    .label("User Attributes") //
                    .defaultValue(null) //
                    .helpText("Comma separated list of user attributes to send with authz requests.") //
                    .add() //

                    .property().name(OpaClient.OPA_USE_REALM_ATTRIBUTES) //
                    .type(ProviderConfigProperty.BOOLEAN_TYPE) //
                    .label("Use realm attributes") //
                    .defaultValue("false") //
                    .helpText("If enabled, realm attributes will be sent with authz requests.") //
                    .add() //

                    .property().name(OpaClient.OPA_REALM_ATTRIBUTES) //
                    .type(ProviderConfigProperty.STRING_TYPE) //
                    .label("Realm Attributes") //
                    .defaultValue(null) //
                    .helpText("Comma separated list of realm attributes to send with authz requests.") //
                    .add() //

                    .property().name(OpaClient.OPA_USE_CLIENT_ATTRIBUTES) //
                    .type(ProviderConfigProperty.BOOLEAN_TYPE) //
                    .label("Use client attributes") //
                    .defaultValue("false") //
                    .helpText("If enabled, client attributes will be sent with authz requests.") //
                    .add() //

                    .property().name(OpaClient.OPA_CLIENT_ATTRIBUTES) //
                    .type(ProviderConfigProperty.STRING_TYPE) //
                    .label("Client Attributes") //
                    .defaultValue(null) //
                    .helpText("Comma separated list of client attributes to send with authz requests.") //
                    .add() //

                    .property().name(OpaClient.OPA_USE_REALM_ROLES) //
                    .type(ProviderConfigProperty.BOOLEAN_TYPE) //
                    .label("Use realm roles") //
                    .defaultValue("true") //
                    .helpText("If enabled, realm roles will be sent with authz requests.") //
                    .add() //

                    .property().name(OpaClient.OPA_USE_CLIENT_ROLES) //
                    .type(ProviderConfigProperty.BOOLEAN_TYPE) //
                    .label("Use client roles") //
                    .defaultValue("true") //
                    .helpText("If enabled, client roles for the current client will be sent with authz requests.") //
                    .add() //

                    .property().name(OpaClient.OPA_USE_GROUPS) //
                    .type(ProviderConfigProperty.BOOLEAN_TYPE) //
                    .label("Use groups") //
                    .defaultValue("true") //
                    .helpText("If enabled, group information will be sent with authz requests.") //
                    .add() //

                    .build();

            CONFIG_PROPERTIES = Collections.unmodifiableList(list);
        }

        public String getId() {
            return "acme-opa-authenticator";
        }

        @Override
        public String getDisplayType() {
            return "Acme: OPA Authentication";
        }

        @Override
        public String getReferenceCategory() {
            return "opa";
        }

        @Override
        public String getHelpText() {
            return "Validates access based on an OPA policy.";
        }

        @Override
        public boolean isConfigurable() {
            return !CONFIG_PROPERTIES.isEmpty();
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
        public Authenticator create(KeycloakSession session) {
            return new OpaAuthenticator(session, opaClient);
        }

        @Override
        public void init(Config.Scope config) {

            this.opaClient = new OpaClient();
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
