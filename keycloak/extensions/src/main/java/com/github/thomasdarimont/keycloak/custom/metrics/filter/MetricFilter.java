package com.github.thomasdarimont.keycloak.custom.metrics.filter;

import com.github.thomasdarimont.keycloak.custom.metrics.RequestMetricsUpdater;
import lombok.extern.jbosslog.JBossLog;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Set;

@JBossLog
public class MetricFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String METRICS_REQUEST_TIMESTAMP = "metrics.requestTimestamp";

    private static final Set<String> IGNORED_PATH_PREFIXES = Set.of("/resources");

    private static final Set<MediaType> CONTENT_TYPES;

    static {
        CONTENT_TYPES = Set.of(
                MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_XML_TYPE,
                MediaType.TEXT_HTML_TYPE
        );
    }

    private final RequestMetricsUpdater requestMetricsUpdater;

    private final boolean uriMetricsEnabled;

    public MetricFilter(boolean uriMetricsEnabled, RequestMetricsUpdater requestMetricsUpdater) {
        this.uriMetricsEnabled = uriMetricsEnabled;
        this.requestMetricsUpdater = requestMetricsUpdater;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.setProperty(METRICS_REQUEST_TIMESTAMP, System.currentTimeMillis());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

        String uri = requestContext.getUriInfo().getPath();
        for (String ignoredPathPrefix : IGNORED_PATH_PREFIXES) {
            if (uri.startsWith(ignoredPathPrefix)) {
                return;
            }
        }

        int status = responseContext.getStatus();
        if (uriMetricsEnabled) {
            requestMetricsUpdater.recordResponse(status, requestContext.getMethod(), uri);
        }

        Long metricsRequestTimestampMillis = (Long) requestContext.getProperty(METRICS_REQUEST_TIMESTAMP);
        if (metricsRequestTimestampMillis == null) {
            return;
        }

        if (!contentTypeIsRelevant(responseContext)) {
            return;
        }

        long requestDurationMillis = System.currentTimeMillis() - metricsRequestTimestampMillis;
        if (uriMetricsEnabled) {
            requestMetricsUpdater.recordRequestDuration(status, requestDurationMillis, requestContext.getMethod(), uri);
        }
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
