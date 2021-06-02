package com.github.thomasdarimont.keycloak.custom;

import com.github.thomasdarimont.keycloak.custom.KeycloakTestSupport.UserRef;
import com.github.thomasdarimont.keycloak.custom.profile.ageinfo.AgeInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.token.TokenService;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class KeycloakIntegrationTest {

    public static final String TEST_REALM = "acme-internal";

    public static final String TEST_CLIENT = "test-client";

    public static final String TEST_USER_PASSWORD = "test";

    public static final KeycloakEnvironment KEYCLOAK_ENVIRONMENT = new KeycloakEnvironment();

    @BeforeAll
    public static void beforeAll() {
        KEYCLOAK_ENVIRONMENT.start();
    }

    @AfterAll
    public static void afterAll() {
        KEYCLOAK_ENVIRONMENT.stop();
    }

    @Test
    public void ageInfoMapperShouldAddAgeClassClaim() throws Exception {

        Keycloak adminClient = KEYCLOAK_ENVIRONMENT.getAdminClient();

        RealmResource acmeRealm = adminClient.realm(TEST_REALM);

        UserRef user22Years = KeycloakTestSupport.createOrUpdateTestUser(acmeRealm, "test-user-age22", TEST_USER_PASSWORD, user -> {
            user.setFirstName("Firstname");
            user.setLastName("Lastname");
            user.setAttributes(ImmutableMap.of("birthdate", List.of(LocalDate.now().minusYears(22).toString())));
        });

        TokenService tokenService = KEYCLOAK_ENVIRONMENT.getTokenService();

        AccessTokenResponse accessTokenResponse = tokenService.grantToken(TEST_REALM, new Form()
                .param("grant_type", "password")
                .param("username", user22Years.getUsername())
                .param("password", TEST_USER_PASSWORD)
                .param("client_id", TEST_CLIENT)
                .param("scope", "openid acme.profile acme.ageinfo")
                .asMap());

//            System.out.println("Token: " + accessTokenResponse.getToken());

        // parse the received id-token
        TokenVerifier<IDToken> verifier = TokenVerifier.create(accessTokenResponse.getIdToken(), IDToken.class);
        verifier.parse();

        // check for the custom claim
        IDToken accessToken = verifier.getToken();
        String ageInfoClaim = (String) accessToken.getOtherClaims().get(AgeInfoMapper.AGE_CLASS_CLAIM);

        assertThat(ageInfoClaim).isNotNull();
        assertThat(ageInfoClaim).isEqualTo("over21");
    }

    @Test
    public void auditListenerShouldPrintLogMessage() throws Exception {

        Assumptions.assumeTrue(KEYCLOAK_ENVIRONMENT.getMode() == KeycloakEnvironment.Mode.TESTCONTAINERS);

        ToStringConsumer consumer = new ToStringConsumer();
        KEYCLOAK_ENVIRONMENT.getKeycloak().followOutput(consumer);

        TokenService tokenService = KEYCLOAK_ENVIRONMENT.getTokenService();

        // trigger user login via ROPC
        tokenService.grantToken(TEST_REALM, new Form()
                .param("grant_type", "password")
                .param("username", "tester")
                .param("password", TEST_USER_PASSWORD)
                .param("client_id", TEST_CLIENT)
                .param("scope", "openid acme.profile acme.ageinfo")
                .asMap());

        // Allow the container log to flush
        TimeUnit.MILLISECONDS.sleep(750);

        assertThat(consumer.toUtf8String()).contains("audit userEvent");
    }

    @Test
    public void pingResourceShouldBeAccessibleForUser() {

        TokenService tokenService = KEYCLOAK_ENVIRONMENT.getTokenService();

        AccessTokenResponse accessTokenResponse = tokenService.grantToken(TEST_REALM, new Form()
                .param("grant_type", "password")
                .param("username", "tester")
                .param("password", TEST_USER_PASSWORD)
                .param("client_id", TEST_CLIENT)
                .param("scope", "openid")
                .asMap());

        String accessToken = accessTokenResponse.getToken();
        System.out.println("Token: " + accessToken);

        CustomResources customResources = KEYCLOAK_ENVIRONMENT.getClientProxy(CustomResources.class);
        Map<String, Object> response = customResources.ping(TEST_REALM, "Bearer " + accessToken);
        System.out.println(response);

        assertThat(response).isNotNull();
        assertThat(response.get("user")).isEqualTo("tester");
    }


    interface CustomResources {

        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @javax.ws.rs.Path("/realms/{realm}/custom-resources/ping")
        Map<String, Object> ping(@PathParam("realm") String realm, @HeaderParam("Authorization") String token);
    }
}
