package com.github.thomasdarimont.keycloak.custom.endpoints.profile;

import lombok.Data;

@Data
public class ProfileData {

    String firstName;

    String lastName;

    String email;
}