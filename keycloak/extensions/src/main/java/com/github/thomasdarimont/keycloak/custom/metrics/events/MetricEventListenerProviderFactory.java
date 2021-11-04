package com.github.thomasdarimont.keycloak.custom.metrics.events;

import com.github.thomasdarimont.keycloak.custom.metrics.KeycloakMetricStore;
import com.github.thomasdarimont.keycloak.custom.metrics.KeycloakMetrics;
import com.github.thomasdarimont.keycloak.custom.metrics.filter.MetricFilter;
import com.github.thomasdarimont.keycloak.custom.metrics.filter.MetricRequestRecorder;
import com.github.thomasdarimont.keycloak.custom.support.KeycloakUtil;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@JBossLog
@AutoService(EventListenerProviderFactory.class)
public class MetricEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final EventListenerProvider INSTANCE = getEventListenerProvider();

    private static EventListenerProvider getEventListenerProvider() {
        if (KeycloakUtil.isRunningOnKeycloak()) {
            return new MetricEventListenerProvider();
        }
        return new NoopEventListenerProvider();
    }

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

        // TODO Add support for metrics in Keycloak.X - we need to uncomment
        MetricRegistry metricRegistry = KeycloakMetrics.lookupMetricRegistry();
//        registerMetrics(factory, metricRegistry);
//
//        // TODO configure robust request metrics collection
//        if (Resteasy.getProvider().getClass().getSimpleName().equals("Resteasy3Provider")) {
//            // metrics filter registration is only supported for Resteasy3
//            registerMetricsFilter(metricRegistry);
//        }
    }

    private void registerMetrics(KeycloakSessionFactory factory, MetricRegistry metricRegistry) {
        log.info("Begin register metrics...");
        KeycloakMetrics metrics = new KeycloakMetrics();

        KeycloakMetricStore metricsStore = new KeycloakMetricStore(factory, metricRegistry, metrics);
        metrics.registerMetrics(metricRegistry, metricsStore);
        log.info("Finished register metrics.");
    }

    protected void registerMetricsFilter(MetricRegistry metricRegistry) {
        log.info("Begin register metrics-filter...");
        MetricFilter metricFilter = new MetricFilter(true, new MetricRequestRecorder(metricRegistry));

        ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
        instance.getContainerRequestFilterRegistry().registerSingleton(metricFilter);
        instance.getContainerResponseFilterRegistry().registerSingleton(metricFilter);
        log.info("Finished register metrics-filter.");
    }

    @Override
    public void close() {

        log.info("Begin unregister metrics...");

        MetricRegistry metricRegistry = KeycloakMetrics.lookupMetricRegistry();
        // remove all metrics on extension reload
        metricRegistry.removeMatching((metricID, metric) -> true);

        log.info("Finished unregister metrics.");
    }

    @Override
    public String getId() {
        return "metrics";
    }

    private static class NoopEventListenerProvider implements EventListenerProvider {

        @Override
        public void onEvent(Event event) {
            // NOOP
            assert true;
        }

        @Override
        public void onEvent(AdminEvent event, boolean includeRepresentation) {
            // NOOP
            assert true;
        }

        @Override
        public void close() {
            // NOOP
            assert true;
        }

    }

    ;

}
