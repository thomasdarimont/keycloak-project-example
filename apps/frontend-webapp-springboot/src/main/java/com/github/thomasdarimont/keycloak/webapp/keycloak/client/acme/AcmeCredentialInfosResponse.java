package com.github.thomasdarimont.keycloak.webapp.keycloak.client.acme;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcmeCredentialInfosResponse {
    private Map<String, List<AcmeCredential>> credentialInfos;
}