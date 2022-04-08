package com.github.thomasdarimont.keycloak.webapp.config;

import com.github.thomasdarimont.keycloak.webapp.keycloak.client.KeycloakClientException;
import com.github.thomasdarimont.keycloak.webapp.keycloak.client.acme.AcmeKeycloakClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class KeycloakClientConfig {

    public static final String REGISTRATION_ID = "keycloak";

    public static ExchangeFilterFunction errorHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is5xxServerError()) {
                return clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody ->
                                Mono.error(new KeycloakClientException(clientResponse.statusCode(), errorBody)));
            } else if (clientResponse.statusCode().is4xxClientError()) {
                return clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody ->
                                Mono.error(new KeycloakClientException(clientResponse.statusCode(), errorBody)));
            } else {
                return Mono.just(clientResponse);
            }
        });
    }

    @Bean
    public AcmeKeycloakClientProperties keycloakServiceProperties(ClientRegistrationRepository clientRegistrations) {

        var keycloak = clientRegistrations.findByRegistrationId(REGISTRATION_ID);
        var providerDetails = keycloak.getProviderDetails();

        return AcmeKeycloakClientProperties.builder()
                .requestTimeout(Duration.ofMillis(3000))
                .clientId(keycloak.getClientId())
                .clientSecret(keycloak.getClientSecret().getBytes(StandardCharsets.UTF_8))
                .authUri(providerDetails.getAuthorizationUri())
                .userInfoUri(providerDetails.getUserInfoEndpoint().getUri())
                .scopes(keycloak.getScopes())
                .settingsPath("/custom-resources/me/settings")
                .accountPath("/custom-resources/me/account")
                .credentialsPath("/custom-resources/me/credentials")
                .applicationsPath("/custom-resources/me/applications")
                .build();
    }

    @Bean
    public OidcUserService oidcUserService() {
        return new OidcUserService();
    }

    @Bean
    public WebClient keycloakWebClient(ClientRegistrationRepository clientRegistrations,
                                       OAuth2AuthorizedClientRepository authorizedClients) {
        var oauth = new ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);
        oauth.setDefaultOAuth2AuthorizedClient(true);

        var clientRegistration = clientRegistrations.findByRegistrationId(REGISTRATION_ID);
        oauth.setDefaultClientRegistrationId(clientRegistration.getRegistrationId());

        return WebClient.builder().apply(oauth.oauth2Configuration())
                .defaultHeaders(headers -> {
                    headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                })
                .filter(errorHandler())
                .baseUrl(clientRegistration.getProviderDetails().getIssuerUri())
                .build();
    }

}
