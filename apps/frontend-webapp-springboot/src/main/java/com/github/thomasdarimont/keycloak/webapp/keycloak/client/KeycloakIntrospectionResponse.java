package com.github.thomasdarimont.keycloak.webapp.keycloak.client;

import lombok.Data;

@Data
public class KeycloakIntrospectionResponse {
    private String active;
    private String token_type;
}
