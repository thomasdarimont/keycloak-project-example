package com.github.thomasdarimont.keycloak.custom.metrics.filter;

import com.github.thomasdarimont.keycloak.custom.metrics.KeycloakMetrics;
import com.github.thomasdarimont.keycloak.custom.metrics.RequestMetricsUpdater;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.metrics.MetricRegistry;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.util.Set;

@Provider // needed for Keycloak.X
@JBossLog
public class MetricFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final boolean RECORD_URI_METRICS_ENABLED;

    private static final String METRICS_REQUEST_TIMESTAMP = "metrics.requestTimestamp";

    private static final Set<String> IGNORED_PATH_PREFIXES = Set.of("/resources");

    private static final Set<MediaType> CONTENT_TYPES;

    static {
        CONTENT_TYPES = Set.of(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_HTML_TYPE);
        RECORD_URI_METRICS_ENABLED = Boolean.getBoolean("acme.keycloak.metrics.record-uri-metrics");
    }

    private final RequestMetricsUpdater requestMetricsUpdater;

    public MetricFilter() {
        this(KeycloakMetrics.lookupMetricRegistry());
    }

    public MetricFilter(MetricRegistry metricRegistry) {
        this.requestMetricsUpdater = new MetricRequestRecorder(metricRegistry);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {

        if (shouldIgnoreRequest(requestContext)) {
            return;
        }

        requestContext.setProperty(METRICS_REQUEST_TIMESTAMP, System.currentTimeMillis());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

        if (shouldIgnoreRequest(requestContext)) {
            return;
        }

        // TODO strip userIds, componentIds, etc. from uri paths to avoid accidental dimension explosion
        int status = responseContext.getStatus();
        String uri = requestContext.getUriInfo().getPath();
        requestMetricsUpdater.recordResponse(uri, requestContext.getMethod(), status);

        Long metricsRequestTimestampMillis = (Long) requestContext.getProperty(METRICS_REQUEST_TIMESTAMP);
        if (metricsRequestTimestampMillis == null) {
            return;
        }

        if (!contentTypeIsRelevant(responseContext)) {
            return;
        }

        // TODO make request duration recording optional
        long requestDurationMillis = System.currentTimeMillis() - metricsRequestTimestampMillis;
        requestMetricsUpdater.recordRequestDuration(uri, requestContext.getMethod(), status, requestDurationMillis);
    }

    private boolean shouldIgnoreRequest(ContainerRequestContext requestContext) {

        if (!RECORD_URI_METRICS_ENABLED) {
            return true;
        }

        String uri = requestContext.getUriInfo().getPath();

        for (String ignoredPathPrefix : IGNORED_PATH_PREFIXES) {
            if (uri.startsWith(ignoredPathPrefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean contentTypeIsRelevant(ContainerResponseContext responseContext) {

        MediaType responseMediaType = responseContext.getMediaType();
        if (responseMediaType == null) {
            return false;
        }

        for (MediaType mediaType : CONTENT_TYPES) {
            if (mediaType.isCompatible(responseMediaType)) {
                return true;
            }
        }

        return false;
    }
}
