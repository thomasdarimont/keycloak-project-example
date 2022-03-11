package com.github.thomasdarimont.keycloak.custom.endpoints.account;

import com.github.thomasdarimont.keycloak.custom.account.AccountActivity;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.DELETE;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

public class AcmeAccountResource {

    private final KeycloakSession session;
    private final AccessToken token;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpRequest request;

    public AcmeAccountResource(KeycloakSession session, AccessToken token) {
        this.session = session;
        this.token = token;
    }

    @OPTIONS
    public Response getCorsOptions() {
        return withCors(request, Response.ok()).build();
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
        if (canAccessAccount) {
            return Response.status(FORBIDDEN).build();
        }

        AccountActivity.onAccountDeletionRequested(session, realm, user, uriInfo);

        var responseBody = new HashMap<String, Object>();

        return withCors(request, Response.ok(responseBody)).build();
    }


    private Cors withCors(HttpRequest request, Response.ResponseBuilder responseBuilder) {

        URI baseUri = URI.create(request.getHttpHeaders().getHeaderString("origin"));
        String origin = baseUri.getHost();
        // TODO use frontend url hostname here
        boolean trustedDomain = origin.endsWith(".acme.test");

        Cors cors = Cors.add(request, responseBuilder);
        if (trustedDomain) {
            cors.allowedOrigins(baseUri.getScheme() + "://" + origin + ":" + baseUri.getPort()); //
        }

        return cors.auth().allowedMethods("GET", "OPTIONS", "DELETE").preflight();
    }
}
