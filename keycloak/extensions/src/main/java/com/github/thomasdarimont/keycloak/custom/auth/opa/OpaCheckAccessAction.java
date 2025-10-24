package com.github.thomasdarimont.keycloak.custom.auth.opa;

import com.github.thomasdarimont.keycloak.custom.config.RealmConfig;
import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Response;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.messages.Messages;

import java.util.Collections;
import java.util.List;

/**
 * Required Action that evaluates an OPA Policy to check if access to target client is allowed for the current user.
 */
public class OpaCheckAccessAction implements RequiredActionProvider {

    public static final String ID = "acme-opa-check-access";

    public static final String ACTION_ALREADY_EXECUTED_MARKER = ID;

    public static final String REALM_ATTRIBUTE_PREFIX = "acme_opa_chk_";

    private final OpaClient opaClient;

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    static {
        var list = ProviderConfigurationBuilder.create() //

                .property().name(OpaClient.OPA_AUTHZ_URL) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("Authz Server Policy URL") //
                .defaultValue(OpaClient.DEFAULT_OPA_AUTHZ_URL) //
                .helpText("URL of OPA Authz Server Policy Resource") //
                .add() //

                .property().name(OpaClient.OPA_USER_ATTRIBUTES) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("User Attributes") //
                .defaultValue(null) //
                .helpText("Comma separated list of user attributes to send with authz requests.") //
                .add() //

                .property().name(OpaClient.OPA_REALM_ATTRIBUTES) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("Realm Attributes") //
                .defaultValue(null) //
                .helpText("Comma separated list of realm attributes to send with authz requests.") //
                .add() //

                .property().name(OpaClient.OPA_CONTEXT_ATTRIBUTES) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("Context Attributes") //
                .defaultValue(null) //
                .helpText("Comma separated list of context attributes to send with authz requests. Supported attributes: remoteAddress") //
                .add() //

                .property().name(OpaClient.OPA_REQUEST_HEADERS) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("Request Headers") //
                .defaultValue(null) //
                .helpText("Comma separated list of request headers to send with authz requests.") //
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
                .helpText("If enabled, client roles will be sent with authz requests.") //
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

    public OpaCheckAccessAction(OpaClient opaClient) {
        this.opaClient = opaClient;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

        var authSession = context.getAuthenticationSession();
        if (authSession.getAuthNote(ACTION_ALREADY_EXECUTED_MARKER) != null) {
            return;
        }
        authSession.setAuthNote(ACTION_ALREADY_EXECUTED_MARKER, "true");

        authSession.addRequiredAction(ID);
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {

        var realm = context.getRealm();
        var user = context.getUser();
        var session = context.getSession();
        var authSession = context.getAuthenticationSession();
        var config = new RealmConfig(realm, REALM_ATTRIBUTE_PREFIX); // realm attributes are looked up with prefix

        var access = opaClient.checkAccess(session, config, realm, user, authSession.getClient(), OpaClient.OPA_ACTION_CHECK_ACCESS);

        if (access.isAllowed()) {
            context.success();
            return;
        }

        // deny access
        var loginForm = session.getProvider(LoginFormsProvider.class);
        var hint = access.getHint();
        if (hint == null) {
            hint = Messages.ACCESS_DENIED;
        }
        loginForm.setError(hint, user.getUsername());

        context.challenge(loginForm.createErrorPage(Response.Status.FORBIDDEN));
        return;

    }

    @Override
    public void processAction(RequiredActionContext context) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }


    @AutoService(RequiredActionFactory.class)
    public static class Factory implements RequiredActionFactory {

        private OpaClient opaClient;

        @Override
        public RequiredActionProvider create(KeycloakSession session) {
            return new OpaCheckAccessAction(opaClient);
        }

        @Override
        public void init(Config.Scope config) {
            // NOOP
            this.opaClient = new OpaClient();
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            // NOOP
        }

        @Override
        public List<ProviderConfigProperty> getConfigMetadata() {
            return CONFIG_PROPERTIES;
        }

        @Override
        public void close() {
            // NOOP
        }

        @Override
        public String getId() {
            return OpaCheckAccessAction.ID;
        }

        @Override
        public String getDisplayText() {
            return "Acme: OPA Check Access";
        }

        @Override
        public boolean isOneTimeAction() {
            return true;
        }
    }
}
