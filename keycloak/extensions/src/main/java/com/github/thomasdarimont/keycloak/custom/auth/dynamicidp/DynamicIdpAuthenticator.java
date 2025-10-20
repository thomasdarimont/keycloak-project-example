package com.github.thomasdarimont.keycloak.custom.auth.dynamicidp;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Checks if the current user
 */
public class DynamicIdpAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        var user = context.getUser();
        if (user == null) {
            context.failure(AuthenticationFlowError.UNKNOWN_USER);
            return;
        }

        var realm = context.getRealm();
        var session = context.getSession();
        var idps = session.identityProviders().getAllStream().map(IdentityProviderModel::getAlias).collect(Collectors.toSet());
        var identityProviderLinks = session.users().getFederatedIdentitiesStream(realm, user) //
                .filter(identity -> idps.contains(identity.getIdentityProvider())) //
                .toList();

        if (identityProviderLinks.isEmpty()) {
            context.attempted();
            return;
        }

        var primaryIdpLink = identityProviderLinks.getFirst();
        var idp = session.identityProviders().getByIdOrAlias(primaryIdpLink.getIdentityProvider());

        var authSession = context.getAuthenticationSession();
        var clientSessionCode = new ClientSessionCode<>(session, realm, authSession);
        clientSessionCode.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());

        var client = session.getContext().getClient();
        var uriInfo = session.getContext().getUri().getBaseUri();
        var loginUrl = Urls.identityProviderAuthnRequest(uriInfo, idp.getAlias(), realm.getName()).toString();
        var uriBuilder = UriBuilder.fromUri(loginUrl);
        uriBuilder.queryParam(Constants.CLIENT_ID, client.getClientId());
        uriBuilder.queryParam(LoginActionsService.SESSION_CODE, clientSessionCode.getOrGenerateCode());
        uriBuilder.queryParam(Constants.TAB_ID, context.getUriInfo().getQueryParameters().getFirst(Constants.TAB_ID));
        uriBuilder.queryParam(OIDCLoginProtocol.LOGIN_HINT_PARAM, primaryIdpLink.getUserName());

        URI targetUri = uriBuilder.build();
        context.forceChallenge(Response.temporaryRedirect(targetUri).build());
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // NOOP
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
    public static class Factory implements AuthenticatorFactory {

        @Override
        public String getId() {
            return "acme-dynamic-idp-selector";
        }

        @Override
        public String getDisplayType() {
            return "Acme: Dynamic IDP Redirect";
        }

        @Override
        public String getHelpText() {
            return "Redirect the user to it's primary IdP if connected";
        }

        @Override
        public String getReferenceCategory() {
            return "idp";
        }

        @Override
        public Authenticator create(KeycloakSession session) {
            return new DynamicIdpAuthenticator();
        }

        @Override
        public boolean isConfigurable() {
            return false;
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
            return Collections.emptyList();
        }


        @Override
        public void init(Config.Scope config) {
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
        }

        @Override
        public void close() {
        }
    }
}
