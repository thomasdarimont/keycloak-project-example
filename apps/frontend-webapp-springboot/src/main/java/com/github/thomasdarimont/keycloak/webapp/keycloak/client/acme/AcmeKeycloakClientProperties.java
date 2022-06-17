package com.github.thomasdarimont.keycloak.webapp.keycloak.client.acme;

import com.github.thomasdarimont.keycloak.webapp.keycloak.client.KeycloakClientProperties;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.util.Set;

@Getter
public class AcmeKeycloakClientProperties extends KeycloakClientProperties {

    private final String settingsPath;
    private final String accountPath;
    private final String credentialsPath;
    private final String applicationsPath;

    @Builder
    public AcmeKeycloakClientProperties(Duration requestTimeout, String clientId, String authUri, String userInfoUri, byte[] clientSecret, Set<String> scopes, String settingsPath, String accountPath, String credentialsPath, String applicationsPath) {
        super(requestTimeout, clientId, authUri, userInfoUri, clientSecret, scopes);
        this.settingsPath = settingsPath;
        this.accountPath = accountPath;
        this.credentialsPath = credentialsPath;
        this.applicationsPath = applicationsPath;
    }
}
