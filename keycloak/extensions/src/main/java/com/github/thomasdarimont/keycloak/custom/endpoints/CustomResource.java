package com.github.thomasdarimont.keycloak.custom.endpoints;

import com.github.thomasdarimont.keycloak.custom.config.RealmConfig;
import com.github.thomasdarimont.keycloak.custom.endpoints.account.AcmeAccountResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.admin.AdminSettingsResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.applications.ApplicationsInfoResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.credentials.UserCredentialsInfoResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.migration.TokenMigrationResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.offline.OfflineSessionPropagationResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.profile.UserProfileResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.settings.UserSettingsResource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;

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

        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();

        Map<String, Object> payload = new HashMap<>();
        payload.put("realm", realm.getName());
        payload.put("user", token == null ? "anonymous" : token.getPreferredUsername());
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("greeting", new RealmConfig(realm).getString("acme_greeting", "Greetings!"));

        return Response.ok(payload).build();
    }

    @Path("me/settings")
    public UserSettingsResource settings() {
        return new UserSettingsResource(session, token);
    }

    @Path("me/credentials")
    public UserCredentialsInfoResource credentials() {
        return new UserCredentialsInfoResource(session, token);
    }

    @Path("me/applications")
    public ApplicationsInfoResource applications() {
        return new ApplicationsInfoResource(session, token);
    }

    @Path("me/profile")
    public UserProfileResource profile() {
        return new UserProfileResource(session, token);
    }

    @Path("me/account")
    public AcmeAccountResource account() {
        return new AcmeAccountResource(session, token);
    }

    @Path("mobile/session-propagation")
    public OfflineSessionPropagationResource sessionPropagation() {
        return new OfflineSessionPropagationResource(session, token);
    }

    /**
     * https://id.acme.test:8443/auth/realms/acme-internal/custom-resources/admin/settings
     *
     * @return
     */
    @Path("admin/settings")
    public AdminSettingsResource adminSettings() {
        KeycloakContext context = session.getContext();
        var authResult = AuthenticationManager.authenticateIdentityCookie(session, context.getRealm(), true);
        if (authResult == null) {
            throw new ErrorResponseException("access_denied", "Admin auth required", Response.Status.FORBIDDEN);
        }

        var localRealmAdminRole = context.getRealm().getClientByClientId("realm-management").getRole("realm-admin");
        if (!authResult.getUser().hasRole(localRealmAdminRole)) {
            var loginForm = session.getProvider(LoginFormsProvider.class);
            throw new WebApplicationException(loginForm.createErrorPage(Response.Status.FORBIDDEN));
        }

        return new AdminSettingsResource(session, authResult);
    }

    /**
     * http://localhost:8080/auth/realms/acme-token-migration/custom-resources/migration/token
     *
     * @return
     */
    @Path("migration/token")
    public TokenMigrationResource migration() {
        return new TokenMigrationResource(session, token);
    }
}
