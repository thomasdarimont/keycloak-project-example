package com.github.thomasdarimont.keycloak.webapp.support.keycloakclient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

@Service
@Configuration
@EnableConfigurationProperties
public class DefaultKeycloakClient implements KeycloakClient {

    private final String keycloakClientId;

    private final String keycloakAuthUri;

    private final byte[] keycloakClientSecret;

    private final Set<String> keycloakClientScopes;

    private final Duration keycloakRequestTimeout;

    private final WebClient client;

    private final OidcUserService keycloakUserService;

    public DefaultKeycloakClient(@Qualifier("keycloakWebClient") WebClient client, ClientRegistrationRepository clientRegistrations, OidcUserService keycloakUserService) {
        this.client = client;

        var keycloak = clientRegistrations.findByRegistrationId("keycloak");

        var providerDetails = keycloak.getProviderDetails();
        this.keycloakAuthUri = providerDetails.getAuthorizationUri();

        this.keycloakClientId = keycloak.getClientId();
        this.keycloakClientSecret = keycloak.getClientSecret().getBytes(StandardCharsets.UTF_8);
        this.keycloakClientScopes = keycloak.getScopes();

        this.keycloakRequestTimeout = Duration.ofSeconds(3);

        this.keycloakUserService = keycloakUserService;
    }

    @Override
    public KeycloakIntrospectResponse introspect(String token) {

        var payload = new LinkedMultiValueMap<String, String>();
        payload.set("client_id", this.keycloakClientId);
        payload.set("client_secret", new String(this.keycloakClientSecret, StandardCharsets.UTF_8));
        payload.set("token", token);

        return this.client.method(HttpMethod.POST) //
                .uri("/protocol/openid-connect/token/introspect") //
                .contentType(MediaType.APPLICATION_FORM_URLENCODED) //
                .body(BodyInserters.fromFormData(payload)) //
                .retrieve() //
                .bodyToMono(KeycloakIntrospectResponse.class) //
                .block(keycloakRequestTimeout);
    }

    @Override
    public KeycloakUserInfo userInfo(OAuth2AuthorizedClient authorizedClient, OidcIdToken oidcIdToken) {

        var oidcUserRequest = new OidcUserRequest(authorizedClient.getClientRegistration(), authorizedClient.getAccessToken(), oidcIdToken);

        var oidcUser = this.keycloakUserService.loadUser(oidcUserRequest);

        var keycloakUserInfo = new KeycloakUserInfo();
        keycloakUserInfo.setName(oidcUser.getClaimAsString("name"));
        keycloakUserInfo.setPreferredUsername(oidcUser.getPreferredUsername());
        keycloakUserInfo.setFirstname(oidcUser.getGivenName());
        keycloakUserInfo.setLastname(oidcUser.getFamilyName());
        keycloakUserInfo.setEmail(oidcUser.getEmail());
        keycloakUserInfo.setEmailVerified(oidcUser.getEmailVerified());
        keycloakUserInfo.setPhoneNumber(oidcUser.getPhoneNumber());
        keycloakUserInfo.setPhoneNumberVerified(oidcUser.getPhoneNumberVerified());
        return keycloakUserInfo;
    }

}
