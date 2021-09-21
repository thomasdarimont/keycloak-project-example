package com.github.thomasdarimont.keycloak.custom.metrics;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public interface RealmMetricsUpdater {

    void updateGlobalMetrics(KeycloakSession session, MetricUpdater metricUpdater, long lastUpdateTimestamp);

    void updateRealmMetrics(KeycloakSession session, MetricUpdater metricUpdater, RealmModel realm, long lastUpdateTimestamp);
}
