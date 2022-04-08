package com.github.thomasdarimont.keycloak.webapp.keycloak.client.acme;

import com.github.thomasdarimont.keycloak.webapp.keycloak.client.KeycloakClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;

import java.util.Map;

public interface AcmeKeycloakClient extends KeycloakClient {
    /* call custom endpoint for settings */
    Map<String, Object> listSettings();

    void updateSettings(Map<String, Object> settings);

    void triggerDeleteAccount();

    AcmeCredentialInfosResponse listCredentials();

    void deleteCredential(AcmeCredentialDeleteRequest credentialDeleteRequest);

    default String createEmailChangeRedirectUrl(String redirectAfter) {
        return buildActionExecutionRedirect("acme-update-email", redirectAfter);
    }

    AcmeApplicationInfoResponse listApplications();
}
