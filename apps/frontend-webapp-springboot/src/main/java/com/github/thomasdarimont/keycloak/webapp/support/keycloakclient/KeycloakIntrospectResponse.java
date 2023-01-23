package com.github.thomasdarimont.keycloak.webapp.support.keycloakclient;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakIntrospectResponse {

    public String active;

    @JsonProperty("token_type")
    public String tokenType;
}
