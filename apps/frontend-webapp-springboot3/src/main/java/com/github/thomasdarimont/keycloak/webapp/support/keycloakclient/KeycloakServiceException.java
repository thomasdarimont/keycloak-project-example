package com.github.thomasdarimont.keycloak.webapp.support.keycloakclient;

import lombok.Data;

@Data
public class KeycloakServiceException extends RuntimeException {
    private final String errorBody;
}
