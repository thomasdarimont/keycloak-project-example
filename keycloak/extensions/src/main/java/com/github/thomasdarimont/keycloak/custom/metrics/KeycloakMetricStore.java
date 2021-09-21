package com.github.thomasdarimont.keycloak.custom.metrics;

import com.google.common.base.Stopwatch;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import static com.github.thomasdarimont.keycloak.custom.metrics.KeycloakMetrics.tag;

/**
 * Store for dynamically computed custom metrics.
 * The metrics collection only happens after a configured refresh interval to minimize overhead.
 */
public class KeycloakMetricStore implements KeycloakMetricAccessor {

    private static final Logger LOG = Logger.getLogger(KeycloakMetricStore.class);

    // TODO read value from configuration
    private static final int CUSTOM_METRICS_REFRESH_INTERVAL_MILLIS = Integer.getInteger("keycloak.metrics.refresh_interval_millis", 5000);

    private final KeycloakSessionFactory sessionFactory;

    private final MetricRegistry metricRegistry;

    private final RealmMetricsUpdater realmMetricsUpdater;

    private volatile long lastUpdateTimestamp;

    private Map<String, Double> metricData;

    public KeycloakMetricStore(KeycloakSessionFactory sessionFactory, MetricRegistry metricRegistry, RealmMetricsUpdater realmMetricsUpdater) {
        this.sessionFactory = sessionFactory;
        this.metricRegistry = metricRegistry;
        this.realmMetricsUpdater = realmMetricsUpdater;
    }

    public Double getMetricValue(Metadata metric) {
        return getMetricValue(metric.getName());
    }

    public Double getMetricValue(String metricName) {

        refreshMetricsIfNecessary();

        Map<String, Double> metricData = this.metricData;
        if (metricData == null) {
            return -1.0;
        }

        Double count = metricData.get(metricName);
        if (count != null) {
            return count;
        }

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

        LOG.trace("Begin collecting custom metrics");

        Stopwatch stopwatch = Stopwatch.createStarted();

        Map<String, Double> metricBuffer = new HashMap<>();

        // extract current metrics here to avoid excessive object creation
        Map<MetricID, Metric> currentMetrics = metricRegistry.getMetrics();

        KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
            // depending on the number of realms this might be expensive!
            collectCustomRealmMetricsIntoBuffer(session, metricBuffer, currentMetrics::containsKey);
        });

        long lastUpdateDurationMillis = stopwatch.elapsed().toMillis();
        LOG.debugf("metrics refresh took %sms", lastUpdateDurationMillis);
        metricBuffer.put(KeycloakMetrics.METRICS_REFRESH.getName(), (double) lastUpdateDurationMillis);

        LOG.trace("Finished collecting custom metrics.");

        return metricBuffer;
    }

    private void collectCustomRealmMetricsIntoBuffer(
            KeycloakSession session,
            Map<String, Double> metricsBuffer,
            Predicate<MetricID> isMetricPresent
    ) {

        MetricUpdater metricUpdater = (metric, value, realm) -> {

            if (value == null) {
                // skip recording empty values
                return;
            }

            Tag[] tags = realm == null ? KeycloakMetrics.emptyTag() : new Tag[]{tag("realm", realm.getName())};
            String metricKey = registerCustomMetricIfMissing(metric, isMetricPresent, tags);
            Double metricValue = value.doubleValue();
            metricsBuffer.put(metricKey, metricValue);
        };

        realmMetricsUpdater.updateGlobalMetrics(session, metricUpdater, lastUpdateTimestamp);

        session.realms().getRealmsStream().forEach(realm -> {
            realmMetricsUpdater.updateRealmMetrics(session, metricUpdater, realm, lastUpdateTimestamp);
        });
    }

    private String registerCustomMetricIfMissing(
            Metadata metric,
            Predicate<MetricID> isMetricPresent,
            Tag... tags
    ) {

        // using a string like metric_name{tag1=value1,tag2=value2} is smaller than MetricID
        String metricKey = toMetricKey(metric.getName(), tags);

        // avoid duplicate metric registration
        boolean metricPresent = isMetricPresent.test(new MetricID(metric.getName(), tags));
        if (metricPresent) {
            return metricKey;
        }

        switch (metric.getTypeRaw()) {
            case GAUGE:
                metricRegistry.register(metric, (Gauge<Double>) () -> getMetricValue(metricKey), tags);
                break;
        }

        return metricKey;
    }

    private static String toMetricKey(String metricName, Tag... tags) {

        // TreeMap for stable tag order -> stable metricKey strings
        Map<String, String> tagMap = new TreeMap<>();
        for (Tag tag : tags) {
            tagMap.put(tag.getTagName(), tag.getTagValue());
        }
        return metricName + tagMap;
    }
}
