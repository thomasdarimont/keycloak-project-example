package com.github.thomasdarimont.keycloak.custom.metrics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class KeycloakMetric {

    private final String name;

    private final String description;

    private final KeycloakMetrics.Level level;
}