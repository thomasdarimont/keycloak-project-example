package com.github.thomasdarimont.keycloak.webapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;

@Configuration
class OidcUserServiceConfig {

    @Bean
    public OidcUserService keycloakUserService() {
        return new OidcUserService();
    }

}
