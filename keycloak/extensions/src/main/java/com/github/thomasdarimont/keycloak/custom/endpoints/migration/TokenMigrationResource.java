package com.github.thomasdarimont.keycloak.custom.endpoints.migration;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.Urls;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.util.TokenUtil;

import java.util.Set;

/**
 * Example for migrating an existing offline session form client-1 to a client-2
 */
@RequiredArgsConstructor
public class TokenMigrationResource {

    private static final Set<String> ALLOWED_MIGRATION_CLIENT_ID_PAIRS = Set.of("client-1:client-2", "client-1:client-3");

    private final KeycloakSession session;

    private final AccessToken token;

    @POST
    public Response migrateToken(Request request, TokenMigrationInput input) {

        // validate token (X)
        // validate source-client
        // validate target-client
        if (!isAllowedMigration(input)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        // lookup current client / user session referenced by token
        var sid = token.getSessionId();
        RealmModel realm = session.getContext().getRealm();
        String issuer = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());
        UserSessionModel userSession = session.sessions().getUserSession(realm, sid);

        ClientModel sourceClient = session.clients().getClientByClientId(realm, token.issuedFor);
        ClientModel targetClient = session.clients().getClientByClientId(realm, input.getTargetClientId());

        AuthenticatedClientSessionModel sourceClientAuthClientSession = userSession.getAuthenticatedClientSessionByClient(sourceClient.getId());
        // propagate new target-client in session
        session.getContext().setClient(targetClient);


        // create new dedicated user session
        UserSessionModel newUserSession = session.sessions().createUserSession(null,
                realm,
                userSession.getUser(),
                userSession.getLoginUsername(),
                session.getContext().getConnection().getRemoteAddr(),
                "impersonate",
                false,
                null,
                null,
                UserSessionModel.SessionPersistenceState.PERSISTENT);

        // convert user session to offline user session
        newUserSession = session.sessions().createOfflineUserSession(newUserSession);

        for(var entry : userSession.getNotes().entrySet()) {
            // TODO filter notes if necessary
            newUserSession.setNote(entry.getKey(), entry.getValue());
        }

        // generate new client session
        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, targetClient, newUserSession);
        AuthenticatedClientSessionModel offlineClientSession = session.sessions().createOfflineClientSession(clientSession, newUserSession);
        offlineClientSession.setNote(OAuth2Constants.SCOPE, sourceClientAuthClientSession.getNote(OAuth2Constants.SCOPE));
        offlineClientSession.setNote(OAuth2Constants.ISSUER, sourceClientAuthClientSession.getNote(OAuth2Constants.ISSUER));

        // generate new access token response (AT+RT) with azp=target-client
        Set<String> clientScopeIds = Set.of();
        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndClientScopeIds(offlineClientSession, clientScopeIds, session);

        var event = new EventBuilder(realm, session);
        event.detail("migration", "true");

        TokenManager tokenManager = new TokenManager();
        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, targetClient, event, this.session, newUserSession, clientSessionCtx);
        responseBuilder.generateAccessToken();
        responseBuilder.getAccessToken().issuer(issuer);
        responseBuilder.getAccessToken().setScope(token.getScope());
        responseBuilder.getAccessToken().issuedFor(targetClient.getClientId());
        responseBuilder.generateRefreshToken();
        responseBuilder.getRefreshToken().issuer(issuer);
        responseBuilder.getRefreshToken().setScope(token.getScope());
        responseBuilder.getRefreshToken().type(TokenUtil.TOKEN_TYPE_OFFLINE);
        responseBuilder.getRefreshToken().issuedFor(targetClient.getClientId());

        // skip generation of access token
        responseBuilder.accessToken(null);

        AccessTokenResponse accessTokenResponse = responseBuilder.build();

        return Response.ok(accessTokenResponse).build();
    }

    private boolean isAllowedMigration(TokenMigrationInput input) {
        return token != null && input != null && ALLOWED_MIGRATION_CLIENT_ID_PAIRS.contains(token.issuedFor + ":" + input.targetClientId);
    }

    @Data
    public static class TokenMigrationInput {

        @JsonProperty("target_client_id")
        String targetClientId;
    }
}
