package com.github.thomasdarimont.keycloak.webapp.keycloak.client;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KeycloakUserInfoResponse {

    private String preferred_username;

    private String family_name;
    private String given_name;

    private String email;

}
