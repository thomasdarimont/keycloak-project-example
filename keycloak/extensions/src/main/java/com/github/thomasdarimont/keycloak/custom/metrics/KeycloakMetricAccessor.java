package com.github.thomasdarimont.keycloak.custom.metrics;

public interface KeycloakMetricAccessor {

    Double getMetricValue(String metricKey);
}
