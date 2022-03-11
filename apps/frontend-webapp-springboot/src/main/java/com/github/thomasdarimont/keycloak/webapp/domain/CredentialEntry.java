package com.github.thomasdarimont.keycloak.webapp.domain;

import lombok.Data;

@Data
public class CredentialEntry {

    String id;

    String label;

    String type;
}
