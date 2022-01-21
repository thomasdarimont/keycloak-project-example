package com.acme.backend.springreactive.config;

import com.acme.backend.springreactive.support.keycloak.KeycloakJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.autoconfigure.security.reactive.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuration applied on all web endpoints defined for this
 * application. Any configuration on specific resources is applied
 * in addition to these global rules.
 */
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
class WebSecurityConfig {

    private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    /**
     * Configures basic security handler per HTTP session.
     * <p>
     * <ul>
     * <li>JWT converted into Spring token</li>
     * </ul>
     *
     * @param http security configuration
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        http
                // csrf disabled for testing
                .csrf() //
                .disable() //
                .authorizeExchange() //
                // CORS requests
                .pathMatchers(HttpMethod.OPTIONS, "/api/**") //
                .permitAll() //
                .matchers(PathRequest.toStaticResources().atCommonLocations()) //
                .permitAll() //
                .matchers(EndpointRequest.to("health")) //
                .permitAll() //
                .matchers(EndpointRequest.to("info")) //
                .permitAll().matchers(EndpointRequest.toAnyEndpoint()) //
                .permitAll() //
                .anyExchange() //
                .authenticated() //

                .and() //
                // Enable OAuth2 Resource Server Support
                .oauth2ResourceServer() //
                // Enable custom JWT handling
                .jwt().jwtAuthenticationConverter(keycloakJwtAuthenticationConverter) //
        ;
        return http.build();
    }
}