package com.acme.backend.quarkus.users;

import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
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

    @GET
    @Path("/me")
    public Object me(@Context UriInfo uriInfo) {

        log.infof("### Accessing %s", uriInfo.getPath());

        Object username = jwt.getClaim("preferred_username");
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Hello " + username);
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
        Map<String, Object> data = new HashMap<>();
        data.put("acme", Map.of("roles", List.of(clientId + "_user")));

        return data;
    }
}