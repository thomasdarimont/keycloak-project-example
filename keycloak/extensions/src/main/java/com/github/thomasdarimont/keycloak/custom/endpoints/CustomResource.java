package com.github.thomasdarimont.keycloak.custom.endpoints;

import com.github.thomasdarimont.keycloak.custom.endpoints.credentials.UserCredentialsInfoResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.settings.UserSettingsResource;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code
 * curl -v http://localhost:8080/auth/realms/acme-apps/custom-resources/ping | jq -C .
 * }
 */
public class CustomResource {

    private final KeycloakSession session;
    private final AccessToken token;

    public CustomResource(KeycloakSession session, AccessToken accessToken) {
        this.session = session;
        this.token = accessToken;
    }

    @GET
    @Path("ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ping() {

        Map<String, Object> payload = new HashMap<>();
        payload.put("realm", session.getContext().getRealm().getName());
        payload.put("user", token == null ? "anonymous" : token.getPreferredUsername());
        payload.put("timestamp", System.currentTimeMillis());

        return Response.ok(payload).build();
    }

    @Path("settings/me")
    public UserSettingsResource settings() {

        var resource = new UserSettingsResource(session, token);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("credentials/me")
    public UserCredentialsInfoResource credentials() {

        var resource = new UserCredentialsInfoResource(session, token);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }
}
