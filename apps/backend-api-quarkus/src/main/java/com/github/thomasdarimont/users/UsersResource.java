package com.github.thomasdarimont.users;

import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
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
    public Object me() {

        log.infof("### Access me");

        Object username = jwt.getClaim("preferred_username");
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Hello " + username);
        return data;
    }

    @GET
    // @RolesAllowed("iam") //TODO add once the remote claim mapper is implemented in Keycloak
    @Path("/claims")
    public Object claims(
            @QueryParam("issuer") String issuer,
            @QueryParam("client_id") String clientId,
            @QueryParam("user_id") String userId
    ) {
        log.infof("### Generating dynamic claims for user. issuer=%s client_id=%s user_id=%s",
                issuer, clientId, userId
        );
        Map<String, Object> data = new HashMap<>();
        data.put("roles", List.of(clientId + "_user"));

        return data;
    }
}