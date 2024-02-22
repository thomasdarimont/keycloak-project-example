package com.github.thomasdarimont.keycloak.custom.endpoints.filter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;

import java.util.Map;

import static com.github.thomasdarimont.keycloak.custom.endpoints.filter.TokenFilter.ACCESS_TOKEN_SESSION_KEY;

/**
 * Example for a resource protected by a request filter.
 * Methods annotated with @AuthFilterBinding execute the filter before executing the method
 */
public class ProtectedResource {

    private final KeycloakSession keycloakSession;

    public ProtectedResource(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    /*
     * Run a token check
     */
    @GET
    @Path("/protected")
    @TokenFilterBinding
    @Produces("application/json")
    public Response protectedResource() {
        return Response.ok(Map.of("secret", keycloakSession.getAttribute(ACCESS_TOKEN_SESSION_KEY))).build();
    }

    /*
     * Run a token check AND an azp check
     */
    @GET
    @Path("/veryprotected")
    @AzpFilterBinding
    @TokenFilterBinding
    @Produces("application/json")
    public Response veryProtectedResource() {
        return Response.ok(Map.of("supersecret", "The magic words are squeamish ossifrage",
                "secret", keycloakSession.getAttribute(ACCESS_TOKEN_SESSION_KEY))).build();
    }

    /*
     * Run no checks at all
     */
    @GET
    @Path("/public")
    @Produces("application/json")
    public Response openResource() {
        return Response.ok(Map.of("secret", "no secrets here")).build();
    }
}
