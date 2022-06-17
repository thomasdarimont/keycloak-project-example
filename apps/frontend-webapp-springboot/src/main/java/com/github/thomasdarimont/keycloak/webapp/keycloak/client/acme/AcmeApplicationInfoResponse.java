package com.github.thomasdarimont.keycloak.webapp.keycloak.client.acme;

import lombok.Data;

import java.util.List;

@Data
public class AcmeApplicationInfoResponse {
    private List<AcmeApplication> clients;
}
