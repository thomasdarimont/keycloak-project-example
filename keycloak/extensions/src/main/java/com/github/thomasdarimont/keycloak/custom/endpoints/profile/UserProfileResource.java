package com.github.thomasdarimont.keycloak.custom.endpoints.profile;

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
import java.util.regex.Pattern;

import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

public class UserProfileResource {

    private static final Pattern NAME_PATTERN = Pattern.compile("[\\w\\d][\\w\\d\\s]{0,64}");

    private final static String firstName = "firstName";
    private final static String lastName = "lastName";

    private final KeycloakSession session;
    private final AccessToken token;

    @Context
    private HttpRequest request;

    public UserProfileResource(KeycloakSession session, AccessToken token) {
        this.session = session;
        this.token = token;
    }

    @OPTIONS
    public Response getCorsOptions() {
        return withCors(request, Response.ok()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readProfile() {

        if (token == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        KeycloakContext context = session.getContext();
        UserModel user = session.users().getUserByUsername(context.getRealm(), token.getPreferredUsername());
        if (user == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        ProfileData profileData = new ProfileData();
        profileData.setFirstName(user.getFirstName());
        profileData.setLastName(user.getLastName());
        profileData.setEmail(user.getEmail());

        return withCors(request, Response.ok(profileData)).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(ProfileData newProfileData) {

        if (token == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        KeycloakContext context = session.getContext();
        UserModel user = session.users().getUserByUsername(context.getRealm(), token.getPreferredUsername());
        if (user == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        ProfileData currentProfileData = new ProfileData();
        currentProfileData.setFirstName(user.getFirstName());
        currentProfileData.setLastName(user.getLastName());

        // TODO compute change between current and new profiledata

        String firstName = newProfileData.getFirstName();
        if (firstName == null || !NAME_PATTERN.matcher(firstName).matches()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        user.setFirstName(firstName);
        String lastName = newProfileData.getLastName();
        if (lastName == null || !NAME_PATTERN.matcher(lastName).matches()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        user.setLastName(lastName);
        // email update must be performed via application initiated required action

        return withCors(request, Response.ok(newProfileData)).build();
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
