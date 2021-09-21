package com.github.thomasdarimont.keycloak.custom.metrics;

import org.eclipse.microprofile.metrics.Metadata;
import org.keycloak.models.RealmModel;

public interface MetricUpdater {

    /**
     * Updates a single metric with the given value in the context of a realm.
     * <p>
     * If the realm is null the metric is consider to be global.
     *
     * @param metric
     * @param value
     * @param realm
     */
    void updateMetricValue(Metadata metric, Number value, RealmModel realm);
}
