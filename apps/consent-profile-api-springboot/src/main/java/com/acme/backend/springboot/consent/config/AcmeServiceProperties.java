package com.acme.backend.springboot.consent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "acme")
public class AcmeServiceProperties {

    private KeycloakJwtProperties jwt = new KeycloakJwtProperties();

    /**
     * Specifies JWT client ID, issuer URI and allowed audiences
     * for validation
     */
    @Getter
    @Setter
    public static class KeycloakJwtProperties {

        private String clientId;

        private String issuerUri;

        private List<String> allowedAudiences;
    }

}
