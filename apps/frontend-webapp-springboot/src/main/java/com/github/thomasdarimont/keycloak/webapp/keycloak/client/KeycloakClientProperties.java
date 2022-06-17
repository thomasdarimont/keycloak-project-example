package com.github.thomasdarimont.keycloak.webapp.keycloak.client;

import lombok.Data;

import java.time.Duration;
import java.util.Set;

@Data
public class KeycloakClientProperties {
    private final Duration requestTimeout;
    private final String clientId;
    private final String authUri;
    private final String userInfoUri;
    private final byte[] clientSecret;
    private final Set<String> scopes;

    String getScopeParam() {
        return String.join(" ", getScopes());
    }
}