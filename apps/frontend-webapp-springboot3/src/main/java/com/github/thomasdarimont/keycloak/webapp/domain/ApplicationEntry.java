package com.github.thomasdarimont.keycloak.webapp.domain;

import lombok.Data;

@Data
public class ApplicationEntry {

    String clientId;

    String name;

    String url;
}
