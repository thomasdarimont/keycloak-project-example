package com.github.thomasdarimont.keycloak.custom.metrics;

import org.eclipse.microprofile.metrics.Metadata;

import java.util.Objects;

public class KeycloakMetric {

    private final Metadata metadata;

    public KeycloakMetric(Metadata metadata) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public String getKey() {
        return metadata.getName();
    }

    public static KeycloakMetric newMetric(Metadata metadata) {
        return new KeycloakMetric(metadata);
    }
}
