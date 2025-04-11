package com.github.thomasdarimont.keycloak.custom.endpoints.demo;

import com.github.thomasdarimont.keycloak.custom.oauth.client.OauthClientCredentialsTokenManager;
import jakarta.persistence.Query;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

import java.util.Map;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DemosResource {

    private final KeycloakSession session;

    public DemosResource(KeycloakSession session) {
        this.session = session;
    }

    /**
     * http://localhost:8080/auth/realms/acme-internal/custom-resources/demos/cached-serviceaccount-token
     *
     * @return
     * @throws Exception
     */
    @Path("cached-serviceaccount-token")
    @GET
    public Response demoCachedServiceAccountToken() throws Exception {

        var clientTokenManager = new OauthClientCredentialsTokenManager();
        clientTokenManager.setTokenUrl("https://id.acme.test:8443/auth/realms/acme-internal/protocol/openid-connect/token");
        clientTokenManager.setScope("openid profile");
        clientTokenManager.setUseCache(true);
        clientTokenManager.setClientId("app-demo-service");
        clientTokenManager.setClientSecret("secret");

        SimpleHttp request = SimpleHttp.doGet("https://id.acme.test:8443/auth/realms/acme-internal/protocol/openid-connect/userinfo", session);
        request.auth(clientTokenManager.getToken(session));
        var data = request.asJson(Map.class);

        return Response.ok(data).build();
    }

    /**
     * http://localhost:8080/auth/realms/acme-internal/custom-resources/demos/slow-query
     *
     * @return
     * @throws Exception
     */
    @Path("slow-query")
    @GET
    public Response demoSlowQuery() throws Exception {

        var provider = session.getProvider(JpaConnectionProvider.class);
        Query nativeQuery = provider.getEntityManager().createNativeQuery("SELECT pg_sleep(5)");
        nativeQuery.getResultList();

        return Response.ok(Map.of("executed", true)).build();
    }
}
