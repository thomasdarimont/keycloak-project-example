package com.github.thomasdarimont.keycloak.custom.endpoints.admin;

import com.github.thomasdarimont.keycloak.custom.endpoints.admin.UserProvisioningResource.UserProvisioningConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import java.time.Instant;
import java.util.Map;

/**
 * Collection of custom Admin Resource Endpoints
 */
public class CustomAdminResource {

    private final KeycloakSession session;

    private final RealmModel realm;

    private final AdminPermissionEvaluator auth;

    private final AdminEventBuilder adminEvent;

    private final UserProvisioningConfig privisioningConfig;

    public CustomAdminResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent, UserProvisioningConfig privisioningConfig) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
        this.privisioningConfig = privisioningConfig;
    }

    /**
     * http://localhost:8080/auth/realms/acme-workshop/custom-admin-resources/example
     *
     * @return
     */
    @Path("/example")
    @GET
    public Response getData() {

        if (auth.users().canView()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        return Response.ok(Map.of("time", Instant.now())).build();
    }

    /**
     * http://localhost:8080/auth/realms/acme-workshop/custom-admin-resources/users
     *
     * @return
     */
    @Path("/users")
    public UserProvisioningResource provisioningResource() {
        return new UserProvisioningResource(session, realm, auth, adminEvent, privisioningConfig);
    }
}
