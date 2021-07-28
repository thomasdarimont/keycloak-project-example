package com.github.thomasdarimont.keycloak.custom.endpoints;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

public class UserSettingsResource {

    private final static String SETTINGS_KEY1 = "setting1";
    private final static String SETTINGS_KEY2 = "setting2";

    private final KeycloakSession session;
    private final AccessToken token;

    @Context
    private HttpRequest request;

    public UserSettingsResource(KeycloakSession session, AccessToken token) {
        this.session = session;
        this.token = token;
    }

    @OPTIONS
    public Response getCorsOptions() {
        return withCors(request, Response.ok()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readSettings() {

        if (token == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        KeycloakContext context = session.getContext();
        UserModel user = session.users().getUserByUsername(context.getRealm(), token.getPreferredUsername());
        if (user == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        Map<String, Object> responseBody = new HashMap<>();

        String value1 = user.getFirstAttribute(SETTINGS_KEY1);
        responseBody.put(SETTINGS_KEY1, value1 == null ? "" : value1);

        String value2 = user.getFirstAttribute(SETTINGS_KEY2);
        responseBody.put(SETTINGS_KEY2, "true".equals(value2) ? "on" : "");

        return withCors(request, Response.ok(responseBody)).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response writeSettings(Map<String, Object> settings) {

        if (token == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        KeycloakContext context = session.getContext();
        UserModel user = session.users().getUserByUsername(context.getRealm(), token.getPreferredUsername());
        if (user == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        String value1 = settings.containsKey(SETTINGS_KEY1) ? String.valueOf(settings.get(SETTINGS_KEY1)) : null;
        if (value1 != null && !Pattern.matches("[\\w\\d]{0,20}", value1)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        user.setSingleAttribute(SETTINGS_KEY1, Objects.requireNonNullElse(value1, ""));

        String value2 = settings.containsKey(SETTINGS_KEY2) ? String.valueOf(settings.get(SETTINGS_KEY2)) : null;
        if (value2 != null) {
            user.setSingleAttribute(SETTINGS_KEY2, "" + Boolean.parseBoolean(value2));
        }

        Map<String, Object> responseBody = new HashMap<>();
        return withCors(request, Response.ok(responseBody)).build();
    }

    private Cors withCors(HttpRequest request, Response.ResponseBuilder responseBuilder) {

        URI baseUri = URI.create(request.getHttpHeaders().getHeaderString("origin"));
        String origin = baseUri.getHost();
        boolean trustedDomain = origin.endsWith(".acme.test");

        Cors cors = Cors.add(request, responseBuilder);
        if (trustedDomain) {
            cors.allowedOrigins(baseUri.getScheme() + "://" + origin + ":" + baseUri.getPort()); //
        }

        return cors.auth().allowedMethods("GET", "PUT", "OPTIONS").preflight();
    }
}
