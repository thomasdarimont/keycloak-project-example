package com.github.thomasdarimont.keycloak.webapp.keycloak.client.acme;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcmeCredential {
    private String credentialId;
    private String credentialType;
    private String credentialLabel;
    private Long createdAt;
    private boolean collection;
    private int count;
    private Map<String, Object> metadata;
}