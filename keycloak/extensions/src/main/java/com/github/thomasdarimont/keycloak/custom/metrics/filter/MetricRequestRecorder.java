package com.github.thomasdarimont.keycloak.custom.metrics.filter;

import com.github.thomasdarimont.keycloak.custom.metrics.RequestMetricsUpdater;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;

import static com.github.thomasdarimont.keycloak.custom.metrics.KeycloakMetrics.tag;

public class MetricRequestRecorder implements RequestMetricsUpdater {

    private final MetricRegistry metricRegistry;

    public MetricRequestRecorder(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void recordResponse(String uri, String method, int status) {

        Tag[] tags = {
                tag("status", String.valueOf(status)),
                tag("method", method),
                tag("uri", uri),
        };

        Counter counter = metricRegistry.counter("keycloak_request_total", tags);
        counter.inc();
    }

    @Override
    public void recordRequestDuration(String uri, String method, int status, long requestDurationMillis) {

        Tag[] tags = {
                tag("status", String.valueOf(status)),
                tag("method", method),
                tag("uri", uri),
        };

        Histogram histogramDuration = metricRegistry.histogram("keycloak_request_duration", tags);
        histogramDuration.update(requestDurationMillis);
    }
}
