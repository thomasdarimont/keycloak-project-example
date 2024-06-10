package com.github.thomasdarimont.keycloak.custom.endpoints.account;

import com.github.thomasdarimont.keycloak.custom.account.AccountActivity;
import com.github.thomasdarimont.keycloak.custom.endpoints.CorsUtils;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.cors.Cors;

import java.util.HashMap;
import java.util.Set;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

public class AcmeAccountResource {

    private final KeycloakSession session;
    private final AccessToken token;

    public AcmeAccountResource(KeycloakSession session, AccessToken token) {
        this.session = session;
        this.token = token;
    }

    @OPTIONS
    public Response getCorsOptions() {
        return withCors(session.getContext().getHttpRequest()).add(Response.ok());
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleAccountDeletionRequest() {

        if (token == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();
        UserModel user = session.users().getUserById(realm, token.getSubject());
        if (user == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        var resourceAccess = token.getResourceAccess();
        AccessToken.Access accountAccess = resourceAccess == null ? null : resourceAccess.get(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        var canAccessAccount = accountAccess != null && (accountAccess.isUserInRole(AccountRoles.MANAGE_ACCOUNT) || accountAccess.isUserInRole(AccountRoles.VIEW_PROFILE));
        if (!canAccessAccount) {
            return Response.status(FORBIDDEN).build();
        }

        var uriInfo = session.getContext().getHttpRequest().getUri();
        AccountActivity.onAccountDeletionRequested(session, realm, user, uriInfo);

        var responseBody = new HashMap<String, Object>();
        var request = context.getHttpRequest();
        return withCors(request).add(Response.ok(responseBody));
    }

    private Cors withCors(HttpRequest request) {
        return CorsUtils.addCorsHeaders(session, request, Set.of("GET", "OPTIONS", "DELETE"), null);
    }
}
