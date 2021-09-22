package com.github.thomasdarimont.keycloak.custom.metrics;

import org.keycloak.models.RealmModel;

public interface MetricUpdater {

    /**
     * Updates a single metric with the given value in the context of a realm.
     * <p>
     * If the realm is null the metric is consider to be global.
     *
     * @param keycloakMetric
     * @param value
     * @param realm
     */
    void updateMetricValue(KeycloakMetric keycloakMetric, Number value, RealmModel realm);
}
