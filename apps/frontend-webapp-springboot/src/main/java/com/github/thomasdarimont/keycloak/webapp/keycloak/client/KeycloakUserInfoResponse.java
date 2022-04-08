package com.github.thomasdarimont.keycloak.webapp.keycloak.client;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class KeycloakUserInfoResponse {

    private String preferred_username;

    private String family_name;
    private String given_name;

    private String email;

    private Map<String, Object> otherClaims = new HashMap<>();

    @JsonAnySetter
    public void setClaim(String name, Object value) {
        otherClaims.put(name, value);
    }

}
