package com.github.thomasdarimont.keycloak.webapp.keycloak.client.acme;

import lombok.Data;

@Data
public class AcmeCredentialDeleteRequest {
    private String credentialType;
    private String credentialId;
}
