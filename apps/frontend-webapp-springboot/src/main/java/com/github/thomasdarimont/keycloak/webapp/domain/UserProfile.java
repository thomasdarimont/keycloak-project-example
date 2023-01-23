package com.github.thomasdarimont.keycloak.webapp.domain;

import lombok.Data;

@Data
public class UserProfile {

    String firstname;

    String lastname;

    String email;

    String phoneNumber;
}
