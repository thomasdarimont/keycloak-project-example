package com.github.thomasdarimont.keycloak.custom.tracing.filter;

import lombok.extern.jbosslog.JBossLog;
import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Custom Tracing Filter which emits context information via MDC logging.
 * If json logging is enabled MDC fields are automatically rendered to the log message.
 * Other log outputs require custom formatting.
 */
@Provider
// automatic provider discovery only works for Keycloak.X, Keycloak-Legacy requires an explicit filter registration.
@JBossLog
public class TracingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String REALMS_PATH_PREFIX = "/realms/";
    public static final String REALM = "realm";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        var path = requestContext.getUriInfo().getPath();
        var method = requestContext.getMethod();

        log.tracef("Before Request: %s %s", method, path);

        if (path.startsWith(REALMS_PATH_PREFIX)) {
            var realmName = getRealmNameFromPath(path);
            MDC.put(REALM, realmName);
        }

        // var spanId = requestContext.getHeaderString("X-SpanID");
        // var traceId = requestContext.getHeaderString("X-TraceID");
        // MDC.put("spanId", spanId);
        // MDC.put("traceId", traceId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

        var method = requestContext.getMethod();
        var path = requestContext.getUriInfo().getPath();
        var status = responseContext.getStatus();

        log.tracef("After Request: %s %s Status: %s", method, path, status);

        MDC.remove(REALM);
        // MDC.remove("spanId");
        // MDC.remove("traceId");
    }

    private String getRealmNameFromPath(String path) {
        var prefixLength = REALMS_PATH_PREFIX.length();
        return path.substring(prefixLength, path.indexOf('/', prefixLength));
    }
}
