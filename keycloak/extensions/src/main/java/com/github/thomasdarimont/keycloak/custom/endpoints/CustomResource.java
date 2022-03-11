package com.github.thomasdarimont.keycloak.custom.endpoints;

import com.github.thomasdarimont.keycloak.custom.config.RealmConfig;
import com.github.thomasdarimont.keycloak.custom.endpoints.account.AcmeAccountResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.applications.ApplicationsInfoResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.credentials.UserCredentialsInfoResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.profile.UserProfileResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.settings.UserSettingsResource;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
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

    @Context
    private ResourceContext resourceContext;

    public CustomResource(KeycloakSession session, AccessToken accessToken) {
        this.session = session;
        this.token = accessToken;
    }

    @GET
    @Path("ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ping() {

        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();

        Map<String, Object> payload = new HashMap<>();
        payload.put("realm", realm.getName());
        payload.put("user", token == null ? "anonymous" : token.getPreferredUsername());
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("greeting", new RealmConfig(realm).getString("acme.greeting", "Greetings!"));

        return Response.ok(payload).build();
    }

    @Path("me/settings")
    public UserSettingsResource settings() {
        return resourceContext.initResource(new UserSettingsResource(session, token));
    }

    @Path("me/credentials")
    public UserCredentialsInfoResource credentials() {
        return resourceContext.initResource(new UserCredentialsInfoResource(session, token));
    }

    @Path("me/applications")
    public ApplicationsInfoResource applications() {
        return resourceContext.initResource(new ApplicationsInfoResource(session, token));
    }

    @Path("me/profile")
    public UserProfileResource profile() {
        return resourceContext.initResource(new UserProfileResource(session, token));
    }

    @Path("me/account")
    public AcmeAccountResource account() {
        return resourceContext.initResource(new AcmeAccountResource(session, token));
    }
}
