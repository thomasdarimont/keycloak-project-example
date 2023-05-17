package com.github.thomasdarimont.keycloak.custom.metrics;

import io.micrometer.core.instrument.Tags;
import lombok.Data;
import org.keycloak.models.RealmModel;

import java.util.Map;

public interface RealmMetricUpdater {

    /**
     * Updates a single metric with the given value in the context of a realm.
     * <p>
     * If the realm is null the metric is consider to be global.
     *
     * @param keycloakMetric
     * @param value
     * @param realm
     */
    void updateMetricValue(KeycloakMetric keycloakMetric, MetricUpdateValue<?> value, RealmModel realm);

    @Data
    class MetricUpdateValue<V> {

        private final V value;
    }

    class MultiMetricUpdateValues extends MetricUpdateValue<Map<Tags, Number>> {

        public MultiMetricUpdateValues(Map<Tags, Number> value) {
            super(value);
        }
    }
}