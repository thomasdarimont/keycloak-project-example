package com.github.thomasdarimont.keycloak.custom.endpoints.filters;

import lombok.extern.jbosslog.JBossLog;
import org.jboss.logging.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.util.UUID;

@JBossLog
public class GlobalRequestResponseFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final GlobalRequestResponseFilter INSTANCE = new GlobalRequestResponseFilter();
    public static final String TRACE_ID = "trace_id";

    @Override
    public void filter(ContainerRequestContext requestContext) {
//        log.infof("Before request: request=%s", requestContext);

//        KeycloakSession keycloakSession = ResteasyProviderFactory.getContextData(KeycloakSession.class);

        String traceId = requestContext.getHeaderString("X-TraceID");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID, traceId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
//        log.infof("After request: request=%s response=%s", requestContext, responseContext);

        MDC.remove(TRACE_ID);
    }
}