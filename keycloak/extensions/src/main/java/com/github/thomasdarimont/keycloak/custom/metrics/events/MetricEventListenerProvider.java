package com.github.thomasdarimont.keycloak.custom.metrics.events;

import com.github.thomasdarimont.keycloak.custom.metrics.KeycloakMetrics;
import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

public class MetricEventListenerProvider implements EventListenerProvider {

    private final MetricEventRecorder recorder;

    public MetricEventListenerProvider(MetricEventRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void onEvent(Event event) {
        recorder.recordEvent(event);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        recorder.recordEvent(event, includeRepresentation);
    }

    @Override
    public void close() {
        // NOOP
    }

    @JBossLog
    @AutoService(EventListenerProviderFactory.class)
    public static class Factory implements EventListenerProviderFactory {

        private EventListenerProvider instance;

        @Override
        public String getId() {
            return "acme-metrics";
        }

        @Override
        public EventListenerProvider create(KeycloakSession session) {
            return instance;
        }

        @Override
        public void init(Config.Scope config) {
            // NOOP
        }

        @Override
        public void postInit(KeycloakSessionFactory sessionFactory) {

//            var metricsEnabled = Configuration.getOptionalBooleanValue(NS_KEYCLOAK_PREFIX.concat("metrics-enabled")).orElse(false);
//            if (!metricsEnabled) {
//                instance = new NoopEventListenerProvider();
//            }
//
//            var keycloakMetrics = new KeycloakMetrics(lookupMeterRegistry(), sessionFactory);
//            keycloakMetrics.registerInstanceMetrics();
//
//            sessionFactory.register(event -> {
//
//                if (event instanceof PostMigrationEvent) {
//                    keycloakMetrics.initialize();
//                } else if (event instanceof RealmModel.RealmRemovedEvent) {
//                    var realmRemoved = (RealmModel.RealmRemovedEvent) event;
//                    keycloakMetrics.removeRealmMetrics(realmRemoved.getRealm());
//                }
//            });
//
//            var metricRecorder = new MetricEventRecorder(keycloakMetrics);
//
//            instance = new MetricEventListenerProvider(metricRecorder);
        }

        protected MeterRegistry lookupMeterRegistry() {
            return Metrics.globalRegistry;
        }

        @Override
        public void close() {
            // NOOP
        }
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
}
