package com.github.thomasdarimont.keycloak.custom.endpoints.filter;

import com.github.thomasdarimont.keycloak.custom.support.KeycloakSessionLookup;
import jakarta.annotation.Priority;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;

import java.io.IOException;

/**
 * Custom request filter for token handling. Executed if a request method is annotated with @AuthFilterBinding
 */
@Priority(0)
@Provider
@TokenFilterBinding
public class TokenFilter implements ContainerRequestFilter {

    public static final String ACCESS_TOKEN_SESSION_KEY = "acme-access-token";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Important: The filter is instantiated only once. KeycloakSession must be local, not a field
        KeycloakSession keycloakSession = KeycloakSessionLookup.currentSession();

        AccessToken accessToken = Tokens.getAccessToken(keycloakSession);

        if (accessToken == null) {
            throw new NotAuthorizedException("Invalid or missing token");
        }

        /*
         * Store the access token in the keycloak session, so that it can be used by the actual
         * resource without having to repeat the validation step
         */
        keycloakSession.setAttribute(ACCESS_TOKEN_SESSION_KEY, accessToken);
    }
}
