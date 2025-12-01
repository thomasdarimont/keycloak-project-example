package com.github.thomasdarimont.keycloak.custom.endpoints.demo;

import com.github.thomasdarimont.keycloak.custom.migration.acmecred.AcmeCredentialModel;
import com.github.thomasdarimont.keycloak.custom.oauth.client.OauthClientCredentialsTokenManager;
import jakarta.persistence.Query;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;

import java.util.Map;
import java.util.UUID;

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

    /**
     * http://localhost:8080/auth/realms/acme-internal/custom-resources/demos/acme-legacy-user
     *
     * @return
     * @throws Exception
     */
    @Path("acme-legacy-user")
    @GET
    public Response demoAcmeUser() throws Exception {

        KeycloakContext context = session.getContext();

        String username = "acme-legacy";
        String userId = UUID.nameUUIDFromBytes(username.getBytes()).toString();
        UserModel acmeUser = session.users().addUser(context.getRealm(), userId, username, true, true);
        acmeUser.setEnabled(true);
        acmeUser.setFirstName("Arne");
        acmeUser.setLastName("Legacy");
        acmeUser.setEmail(username + "@acme.test");

        var credModel = new CredentialModel();
        credModel.setType("acme-password");
        credModel.setCreatedDate(Time.currentTimeMillis());
        credModel.setCredentialData("""
                {"algorithm":"acme-sha1", "additionalParameters":{}}
                """);
        // passw0rd
        credModel.setSecretData("""
                {"value":"0a66d1c3549605506df64337ece6e1953ddd09b7:mysalt", "salt":null, "additionalParameters":{}}
                """);
        var acmeModel = AcmeCredentialModel.createFromCredentialModel(credModel);

        CredentialModel storedCredential = acmeUser.credentialManager().createStoredCredential(acmeModel);

        return Response.ok(Map.of("username", username, "userId", userId)).build();
    }

    @Path("component-provider-lookup")
    @GET
    public Response componentProviderLookupExample() throws Exception {
        KeycloakContext context = session.getContext();

        ComponentModel componentModel = context.getRealm().getComponentsStream(context.getRealm().getId(), UserStorageProvider.class.getName()).findFirst().orElse(null);
        String componentId = componentModel.getId();
//        String componentId = "8c309ce1-08cd-4fce-8b29-884d65603cbb";
//        UserStorageProvider storageProvider = session.getComponentProvider(UserStorageProvider.class, componentId);
        session.getProvider(UserStorageProvider.class, componentModel);

        return Response.ok(Map.of("componentId", componentId)).build();
    }
}
