package com.github.thomasdarimont.keycloak.webapp.config;

import com.github.thomasdarimont.keycloak.webapp.support.keycloakclient.KeycloakServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
class KeycloakWebClientConfig {

    @Bean
    @Qualifier("keycloakWebClient")
    public WebClient keycloakWebClient(ClientRegistrationRepository clientRegistrations, OAuth2AuthorizedClientRepository authorizedClients) {

        var oauthExchangeFilterFunction = new ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);
        oauthExchangeFilterFunction.setDefaultOAuth2AuthorizedClient(true);

        var clientRegistration = clientRegistrations.findByRegistrationId("keycloak");
        oauthExchangeFilterFunction.setDefaultClientRegistrationId(clientRegistration.getRegistrationId());

        return WebClient.builder() //
                .apply(oauthExchangeFilterFunction.oauth2Configuration()) //
                .defaultHeaders(headers -> {
                    headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                }) //
                .filter(errorHandler()) //
                .baseUrl(clientRegistration.getProviderDetails().getIssuerUri()) //
                .build();
    }

    public static ExchangeFilterFunction errorHandler() {

        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {

            if (clientResponse.statusCode().is5xxServerError()) {
                return clientResponse.bodyToMono(String.class) //
                        .flatMap(errorBody -> Mono.error(new KeycloakServiceException(errorBody)));
            }

            if (clientResponse.statusCode().is4xxClientError()) {
                return clientResponse.bodyToMono(String.class) //
                        .flatMap(errorBody -> Mono.error(new KeycloakServiceException(errorBody)));
            }

            return Mono.just(clientResponse);
        });
    }

}
