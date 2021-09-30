package com.github.thomasdarimont.keycloak.custom.metrics;

public interface RequestMetricsUpdater {

    void recordResponse(String uri, String method, int status);

    void recordRequestDuration(String uri, String method, int status, long requestDurationMillis);

}
