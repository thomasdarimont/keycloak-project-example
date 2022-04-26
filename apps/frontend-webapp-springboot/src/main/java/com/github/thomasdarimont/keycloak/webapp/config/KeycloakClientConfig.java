package com.github.thomasdarimont.keycloak.webapp.config;

import com.github.thomasdarimont.keycloak.webapp.keycloak.client.KeycloakClientException;
import com.github.thomasdarimont.keycloak.webapp.keycloak.client.acme.AcmeKeycloakClientProperties;
import com.github.thomasdarimont.keycloak.webapp.keycloak.client.acme.DefaultAcmeKeycloakClient;
import com.github.thomasdarimont.keycloak.webapp.support.OAuth2Accessor;
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
class KeycloakClientConfig {

    public static final String REGISTRATION_ID = "keycloak";

    public static ExchangeFilterFunction errorHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {

            if (!response.statusCode().isError()) {
                return Mono.just(response);
            }

            return response.bodyToMono(String.class) //
                    .defaultIfEmpty(response.statusCode().getReasonPhrase()) //
                    .flatMap(errorBody -> //
                            Mono.error(new KeycloakClientException(response.statusCode(), errorBody)));
        });
    }

    @Bean
    public AcmeKeycloakClientProperties keycloakServiceProperties(ClientRegistrationRepository clientRegistrations) {

        var keycloak = clientRegistrations.findByRegistrationId(REGISTRATION_ID);
        var providerDetails = keycloak.getProviderDetails();

        return AcmeKeycloakClientProperties.builder() //
                .requestTimeout(Duration.ofMillis(3000)) //
                .clientId(keycloak.getClientId()) //
                .clientSecret(keycloak.getClientSecret().getBytes(StandardCharsets.UTF_8)) //
                .authUri(providerDetails.getAuthorizationUri()) //
                .userInfoUri(providerDetails.getUserInfoEndpoint().getUri()) //
                .scopes(keycloak.getScopes()) //
                .settingsPath("/custom-resources/me/settings") //
                .accountPath("/custom-resources/me/account") //
                .credentialsPath("/custom-resources/me/credentials") //
                .applicationsPath("/custom-resources/me/applications") //
                .build();
    }

    @Bean
    public DefaultAcmeKeycloakClient keycloakClient(
            WebClient webClient, //
            OidcUserService userService, //
            AcmeKeycloakClientProperties clientProperties, //
            OAuth2Accessor oauth2Accessor //
    ) {
        return new DefaultAcmeKeycloakClient(webClient, userService, clientProperties, oauth2Accessor);
    }

    @Bean
    public OidcUserService oidcUserService() {
        return new OidcUserService();
    }

    @Bean
    public WebClient keycloakWebClient(ClientRegistrationRepository clientRegistrations, OAuth2AuthorizedClientRepository authorizedClients) {
        var oauth = new ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);
        oauth.setDefaultOAuth2AuthorizedClient(true);

        var clientRegistration = clientRegistrations.findByRegistrationId(REGISTRATION_ID);
        oauth.setDefaultClientRegistrationId(clientRegistration.getRegistrationId());

        return WebClient.builder() //
                .apply(oauth.oauth2Configuration()) //
                .defaultHeaders(headers -> {
                    headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                }) //
                .filter(errorHandler()) //
                .baseUrl(clientRegistration.getProviderDetails().getIssuerUri()) //
                .build();
    }

}
