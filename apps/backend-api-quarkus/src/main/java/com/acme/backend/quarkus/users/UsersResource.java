package com.acme.backend.quarkus.users;

import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class UsersResource {

    @Inject
    Logger log;

    @Inject
    JsonWebToken jwt;

    @Context
    SecurityContext securityContext;

    @Context
    UriInfo uriInfo;

    @Context
    HttpRequest request;

    @GET
    @Path("/me")
    public Object me() {

        log.infof("### Accessing %s", uriInfo.getPath());

        // Note in order to have role information in the token, you need to add the microprofile-jwt scope
        // to the token to populate the groups claim with the realm roles.
        // securityContext.isUserInRole("admin");

        //Object username = jwt.getClaim("preferred_username");
        String username = securityContext.getUserPrincipal().getName();

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Hello " + username);
        data.put("backend", "Quarkus");
        data.put("datetime", Instant.now());
        return data;
    }

    @GET
    @RolesAllowed("iam") // require 'iam' present in groups claim list
    @Path("/claims")
    public Object claims(
            @QueryParam("issuer") String issuer,
            @QueryParam("clientId") String clientId,
            @QueryParam("userId") String userId,
            @QueryParam("username") String username
    ) {
        log.infof("### Generating dynamic claims for user. issuer=%s client_id=%s user_id=%s username=%s",
                issuer, clientId, userId, username
        );

        var acmeData = new HashMap<String, Object>();
        acmeData.put("roles", List.of(clientId + "_user"));
        acmeData.put("foo", "bar");

        return Map.of("acme", acmeData);
    }
}