package com.github.thomasdarimont.keycloak.custom.redirect;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Filter that redirects clients requesting URLs with the legacy root path "/auth".
 */
@JBossLog
@PreMatching
@Provider
public class RedirectFilter implements ContainerRequestFilter {

    private static final String CONFIG_LEGACY_REDIRECT_ENABLED = "legacy-root-path.redirect.enabled";

    private static final String CONFIG_LEGACY_REDIRECT_ROOT_PATH = "legacy-root-path.redirect.path";

    private static final boolean DEFAULT_LEGACY_REDIRECT_ENABLED = true;
    private static final String DEFAULT_LEGACY_ROOT_PATH = "/auth";

    private final boolean redirectEnabled;
    private final String legacyRootPath;

    public RedirectFilter() {
        var config = Configuration.getConfig();
        this.redirectEnabled = config.getOptionalValue(CONFIG_LEGACY_REDIRECT_ENABLED, Boolean.class)
                .orElse(DEFAULT_LEGACY_REDIRECT_ENABLED);
        this.legacyRootPath = config.getOptionalValue(CONFIG_LEGACY_REDIRECT_ROOT_PATH, String.class)
                .orElse(DEFAULT_LEGACY_ROOT_PATH) + "/";
    }

    @Override
    public void filter(ContainerRequestContext context) {
        if (!redirectEnabled) {
            return;
        }

        var uriInfo = context.getUriInfo();
        var requestPath = uriInfo.getPath();

        log.tracef("Processing request: %s", uriInfo.getRequestUri());

        if (requestPath.startsWith(legacyRootPath)) {
            var newRequestPath = removeLegacyRootPath(requestPath);

            var builder = uriInfo.getRequestUriBuilder();
            builder.replacePath(newRequestPath);
            var redirectUri = builder.build();

            var response = Response.status(Response.Status.PERMANENT_REDIRECT).location(redirectUri).build();
            context.abortWith(response);

            log.tracef("Redirecting request %s to %s", uriInfo.getRequestUri(), redirectUri);
        }
    }

    private String removeLegacyRootPath(String requestPath) {
        return requestPath.substring(legacyRootPath.length() - 1);
    }
}
