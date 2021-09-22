package com.github.thomasdarimont.keycloak.custom.metrics.events;

import com.github.thomasdarimont.keycloak.custom.metrics.KeycloakMetricStore;
import com.github.thomasdarimont.keycloak.custom.metrics.KeycloakMetrics;
import com.google.auto.service.AutoService;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@AutoService({EventListenerProviderFactory.class})
public class MetricEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final MetricEventListenerProvider INSTANCE = new MetricEventListenerProvider();

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return INSTANCE;
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

        MetricRegistry metricRegistry = KeycloakMetrics.lookupMetricRegistry();

        KeycloakMetrics metrics = new KeycloakMetrics();

        KeycloakMetricStore metricsStore = new KeycloakMetricStore(factory, metricRegistry, metrics);
        metrics.registerMetrics(metricRegistry, metricsStore);
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public String getId() {
        return "metrics";
    }
}
