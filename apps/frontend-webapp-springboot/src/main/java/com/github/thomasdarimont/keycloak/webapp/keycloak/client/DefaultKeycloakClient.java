package com.github.thomasdarimont.keycloak.webapp.keycloak.client;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

public class DefaultKeycloakClient implements KeycloakClient {

    protected final WebClient client;

    protected final KeycloakClientProperties properties;

    private final OidcUserService userService;

    public DefaultKeycloakClient(WebClient client, OidcUserService userService, KeycloakClientProperties properties) {
        this.client = client;
        this.userService = userService;
        this.properties = properties;
    }

    @Override
    public String buildActionExecutionRedirect(String action, String redirectAfter) {
        var uriBuilder = UriComponentsBuilder.newInstance();
        return uriBuilder.uri(URI.create(properties.getAuthUri()))
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", redirectAfter)
                .queryParam("state", UUID.randomUUID())
                //.queryParam("response_mode", "fragment") TODO neeeded?
                .queryParam("response_type", "code")
                .queryParam("scope", properties.getScopeParam())
                .queryParam("nonce", UUID.randomUUID())
                .queryParam("kc_action", action)
                .toUriString();
    }

    @Override
    public KeycloakIntrospectionResponse introspect(String tokenValue) {
        var parameter = new LinkedMultiValueMap<String, String>();
        parameter.set("client_id", properties.getClientId());
        parameter.set("client_secret", new String(properties.getClientSecret(), StandardCharsets.UTF_8));
        parameter.set("token", tokenValue);


        return this.client.method(HttpMethod.POST)
                .uri("/protocol/openid-connect/token/introspect")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                        BodyInserters.fromFormData(parameter))
                .retrieve()
                .bodyToMono(KeycloakIntrospectionResponse.class)
                .block(properties.getRequestTimeout());
    }

    @Override
    public KeycloakUserInfoResponse userInfoViaProvider(OAuth2AuthorizedClient authorizedClient, OidcIdToken oidcIdToken) {
        var userRequest = new OidcUserRequest(authorizedClient.getClientRegistration(), authorizedClient.getAccessToken(), oidcIdToken);
        var oidcUser = this.userService.loadUser(userRequest);
        var keycloakUserInfoResponse = new KeycloakUserInfoResponse();
        keycloakUserInfoResponse.setPreferred_username(oidcUser.getPreferredUsername());
        keycloakUserInfoResponse.setFamily_name(oidcUser.getGivenName());
        keycloakUserInfoResponse.setGiven_name(oidcUser.getFamilyName());
        keycloakUserInfoResponse.setEmail(oidcUser.getEmail());
        return keycloakUserInfoResponse;
    }

    @Override
    public KeycloakUserInfoResponse userInfoDirect(OAuth2AuthorizedClient authorizedClient) {
        return this.client.get()
                .uri(properties.getUserInfoUri())
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(KeycloakUserInfoResponse.class)
                .block(properties.getRequestTimeout());
    }
}
