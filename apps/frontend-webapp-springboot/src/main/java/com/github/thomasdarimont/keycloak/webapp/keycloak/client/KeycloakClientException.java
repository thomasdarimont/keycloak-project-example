package com.github.thomasdarimont.keycloak.webapp.keycloak.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@RequiredArgsConstructor
public class KeycloakClientException extends RuntimeException {
    private final HttpStatus statusCode;
    private final String errorBody;
}
