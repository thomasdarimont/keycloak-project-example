package com.github.thomasdarimont.keycloak.webapp.keycloak.client.acme;

import lombok.Data;

/* see org.keycloak.representations.account.ClientRepresentation */
@Data
public class AcmeApplication {
    private String clientId;
    private String clientName;
    private String effectiveUrl;
}
