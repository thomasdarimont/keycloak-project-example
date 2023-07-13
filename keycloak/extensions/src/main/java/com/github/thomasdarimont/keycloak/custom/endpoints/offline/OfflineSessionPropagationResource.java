package com.github.thomasdarimont.keycloak.custom.endpoints.offline;

import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.util.ResolveRelative;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

@JBossLog
public class OfflineSessionPropagationResource {

    public static final int DEFAULT_TOKEN_VALIDITY_IN_SECONDS = 30;

    private final KeycloakSession session;

    private final AccessToken token;

    @Context
    private HttpRequest request;

    public OfflineSessionPropagationResource(KeycloakSession session, AccessToken token) {
        this.session = session;
        this.token = token;
    }

    /**
     * Generates an ActionToken to propagate the current offline session to an online session for the given target client_id.
     *
     * <pre>
     *   KC_OFFLINE_ACCESS_TOKEN="ey...."
     *   # For transient user session (session cookie)
     *   curl -k -v -H "Authorization: Bearer $KC_OFFLINE_ACCESS_TOKEN" -d "client_id=app-minispa" https://id.acme.test:8443/auth/realms/acme-internal/custom-resources/mobile/session-propagation | jq -C .
     *
     *   # For persistent user session (persistent cookie)
     *   curl -k -v -H "Authorization: Bearer $KC_OFFLINE_ACCESS_TOKEN" -d "client_id=app-minispa" -d "rememberMe=true" https://id.acme.test:8443/auth/realms/acme-internal/custom-resources/mobile/session-propagation | jq -C .
     * </pre>
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response propagateSession(@FormParam("client_id") String targetClientId, @FormParam("rememberMe") Boolean rememberMe) {

        // validate token
        if (token == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        var context = session.getContext();
        var realm = context.getRealm();
        var offlineUserSession = session.sessions().getOfflineUserSession(realm, token.getSessionId());
        if (offlineUserSession == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        var sourceClientId = token.getIssuedFor();
        // TODO validate sourceClientId (is source allowed to propagate session tokens?)

        if (targetClientId == null) {
            return Response.status(BAD_REQUEST).build();
        }

        var targetClient = session.clients().getClientByClientId(realm, targetClientId);
        if (targetClient == null) {
            return Response.status(BAD_REQUEST).build();
        }
        // TODO validate target client (is target allowed for source?)

        var user = offlineUserSession.getUser();

        var targetUri = resolveBaseUri(targetClient);

        var userId = user.getId();
        int absoluteExpirationInSecs = Time.currentTime() + DEFAULT_TOKEN_VALIDITY_IN_SECONDS;
        var actionToken = new SessionPropagationActionToken(userId, absoluteExpirationInSecs, targetClientId, targetUri.toString(), sourceClientId, rememberMe);
        var actionTokenString = actionToken.serialize(session, realm, context.getUri());
        var uriBuilder = LoginActionsService.actionTokenProcessor(session.getContext().getUri()).queryParam(Constants.KEY, actionTokenString);
        var actionTokenLink = uriBuilder.build(realm.getName()).toString();

        log.infof("User requested Offline-Session to User-Session propagation. realm=%s userId=%s sourceClientId=%s targetClientId=%s", realm.getName(), user.getId(), sourceClientId, targetClientId);

        return Response.ok(Map.of("actionLink", actionTokenLink)).build();
    }

    private URI resolveBaseUri(ClientModel targetClient) {
        URI targetUri;
        if (targetClient.getRootUrl() != null && (targetClient.getBaseUrl() == null || targetClient.getBaseUrl().isEmpty())) {
            targetUri = KeycloakUriBuilder.fromUri(targetClient.getRootUrl()).build();
        } else {
            targetUri = KeycloakUriBuilder.fromUri(ResolveRelative.resolveRelativeUri(session, targetClient.getRootUrl(), targetClient.getBaseUrl())).build();
        }
        return targetUri;
    }
}
