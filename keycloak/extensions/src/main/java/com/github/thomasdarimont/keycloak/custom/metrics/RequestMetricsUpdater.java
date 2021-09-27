package com.github.thomasdarimont.keycloak.custom.metrics;

public interface RequestMetricsUpdater {

    void recordResponse(int status, String method, String uri);

    void recordRequestDuration(int status, long requestDurationMillis, String method, String uri);

}
