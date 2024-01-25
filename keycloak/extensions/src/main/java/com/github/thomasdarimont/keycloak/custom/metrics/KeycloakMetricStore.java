package com.github.thomasdarimont.keycloak.custom.metrics;

import com.github.thomasdarimont.keycloak.custom.metrics.RealmMetricUpdater.MetricUpdateValue;
import com.github.thomasdarimont.keycloak.custom.metrics.RealmMetricUpdater.MultiMetricUpdateValues;
import com.google.common.base.Stopwatch;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


/**
 * Store for dynamically computed custom metrics.
 * The metrics collection only happens after a configured refresh interval to minimize overhead.
 */
@JBossLog
public class KeycloakMetricStore implements KeycloakMetricAccessor {

    // TODO read value from configuration
    private static final int CUSTOM_METRICS_REFRESH_INTERVAL_MILLIS = Integer.getInteger("keycloak.metrics.refresh_interval_millis", 5000);

    private final KeycloakSessionFactory sessionFactory;

    private final MeterRegistry meterRegistry;

    private final RealmMetricsUpdater realmMetricsUpdater;

    private volatile long lastUpdateTimestamp;

    private Map<String, Double> metricData;

    public KeycloakMetricStore(KeycloakSessionFactory sessionFactory, MeterRegistry meterRegistry, RealmMetricsUpdater realmMetricsUpdater) {
        this.sessionFactory = sessionFactory;
        this.meterRegistry = meterRegistry;
        this.realmMetricsUpdater = realmMetricsUpdater;
    }

    public Double getMetricValue(String metricKey) {

        refreshMetricsIfNecessary();

        Map<String, Double> metricData = this.metricData;
        if (metricData == null) {
            return -1.0;
        }

        Double count = metricData.get(metricKey);
        if (count != null) {
            return count;
        }

        // metric no longer present
//        MetricID metricID = toMetricId(meterId);
//        boolean removed = meterRegistry.remove(metricID);

        return -1.0;
    }

    private boolean isRefreshNecessary() {

        if (metricData == null) {
            return true;
        }

        long millisSinceLastUpdate = System.currentTimeMillis() - lastUpdateTimestamp;
        return millisSinceLastUpdate > CUSTOM_METRICS_REFRESH_INTERVAL_MILLIS;
    }

    private void refreshMetricsIfNecessary() {

        if (!isRefreshNecessary()) {
            return;
        }

        synchronized (this) {
            if (!isRefreshNecessary()) {
                return;
            }
            this.metricData = refreshMetrics();
            this.lastUpdateTimestamp = System.currentTimeMillis();
        }
    }

    private Map<String, Double> refreshMetrics() {

        log.trace("Begin collecting custom metrics");

        Stopwatch stopwatch = Stopwatch.createStarted();

        Map<String, Double> metricBuffer = new HashMap<>();

        KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
            // depending on the number of realms this might be expensive!
            collectCustomRealmMetricsIntoBuffer(session, metricBuffer);
        });

        long lastUpdateDurationMillis = stopwatch.elapsed().toMillis();
        log.debugf("metrics refresh took %sms", lastUpdateDurationMillis);
        metricBuffer.put(KeycloakMetrics.INSTANCE_METRICS_REFRESH.getName(), (double) lastUpdateDurationMillis);

        log.trace("Finished collecting custom metrics.");

        return metricBuffer;
    }

    private void collectCustomRealmMetricsIntoBuffer(KeycloakSession session, Map<String, Double> metricsBuffer) {

        RealmMetricUpdater metricUpdater = (metric, value, realm) -> {

            if (value == null) {
                // skip recording empty values
                return;
            }

            if (value instanceof MultiMetricUpdateValues) {

                Map<Tags, Number> tagsToMetrics = ((MultiMetricUpdateValues) value).getValue();
                Tags realmTags = realm == null ? Tags.empty() : Tags.of("realm", realm.getName());
                for (var entry : tagsToMetrics.entrySet()) {
                    Tags tags = entry.getKey();
                    Number val = entry.getValue();

                    var metricTags = Tags.concat(realmTags, tags);
                    String metricKey = registerCustomMetricIfMissing(metric, metricTags);
                    Double metricValue = val.doubleValue();
                    metricsBuffer.put(metricKey, metricValue);
                }
            } else if (value instanceof MetricUpdateValue) {

                Tags tags = realm == null ? Tags.empty() : Tags.of("realm", realm.getName());
                String metricKey = registerCustomMetricIfMissing(metric, tags);
                @SuppressWarnings("unchecked")
                Double metricValue = ((MetricUpdateValue<? extends Number>) value).getValue().doubleValue();
                metricsBuffer.put(metricKey, metricValue);

            }
        };

        realmMetricsUpdater.updateGlobalMetrics(session, metricUpdater, lastUpdateTimestamp);

        session.realms().getRealmsStream().forEach(realm -> {
            realmMetricsUpdater.updateRealmMetrics(session, metricUpdater, realm, lastUpdateTimestamp);
        });
    }

    private String registerCustomMetricIfMissing(KeycloakMetric metric, Tags tags) {

        // using a string like metric_name{tag1=value1,tag2=value2} is smaller than MetricID
        String metricKey = toMetricKey(metric.getName(), tags);

        // avoid duplicate metric registration
        Gauge gauge = meterRegistry.find(metric.getName()).tags(tags).gauge();
        boolean metricPresent = gauge != null;
        if (metricPresent) {
            return metricKey;
        }

        Gauge.builder(metric.getName(), () -> getMetricValue(metricKey)) //
                .description(metric.getDescription()) //
                .tags(tags) //
                .register(meterRegistry);

        return metricKey;
    }

    private static String toMetricKey(String metricName, Tags tags) {

        // TreeMap for stable tag order -> stable metricKey strings
        Map<String, String> tagMap = new TreeMap<>();
        for (Tag tag : tags) {
            tagMap.put(tag.getKey(), tag.getValue());
        }
        return metricName + tagMap;
    }
}