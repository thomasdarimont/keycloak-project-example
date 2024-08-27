package com.github.thomasdarimont.keycloak.custom.endpoints.branding;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Optional;

public class BrandingResource {

    private final KeycloakSession session;

    public BrandingResource(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("css")
    @Consumes(MediaType.WILDCARD)
    @Produces("text/css")
    public Response getBranding() {

        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();
        String brackgroundColor = Optional.ofNullable(realm.getAttribute("custom.branding.backgroundColor")).orElse("grey");

        String css = """
                .login-pf body {
                    background-color: %s;
                }
                """.formatted(brackgroundColor);
        return Response.ok(css).build();
    }
}
