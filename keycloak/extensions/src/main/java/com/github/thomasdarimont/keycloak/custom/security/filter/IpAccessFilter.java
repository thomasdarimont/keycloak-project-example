package com.github.thomasdarimont.keycloak.custom.security.filter;

import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import lombok.Data;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.config.Config;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.utils.StringUtil;

import java.net.InetSocketAddress;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Filter to restrict access to Keycloak Endpoints via CIDR IP ranges.
 */
@JBossLog
@Provider
public class IpAccessFilter implements ContainerRequestFilter {

    public static final String DEFAULT_IP_FILTER_RULES = "127.0.0.1/24,192.168.80.1/16,172.0.0.1/8";
    public static final String ADMIN_IP_FILTER_RULES_ALLOW = "acme.keycloak.admin.ip-filter-rules.allow";
    public static final ForbiddenException FORBIDDEN_EXCEPTION = new ForbiddenException();
    public static final Pattern SLASH_SPLIT_PATTERN = Pattern.compile("/");
    public static final Pattern COMMA_SPLIT_PATTERN = Pattern.compile(",");

    private final PathIpFilterRules adminPathIpFilterRules;

    @Context
    private HttpServerRequest httpServerRequest;

    public IpAccessFilter() {
        this.adminPathIpFilterRules = createAdminIpFilterRules(Configuration.getConfig());
    }

    private PathIpFilterRules createAdminIpFilterRules(Config config) {

        var contextPath = config.getValue("quarkus.http.root-path", String.class);
        var adminPath = makeContextPath(contextPath, "admin");
        var filterRules = config //
                .getOptionalValue(ADMIN_IP_FILTER_RULES_ALLOW, String.class) //
                .orElse(DEFAULT_IP_FILTER_RULES);

        if (StringUtil.isBlank(filterRules)) {
            return null;
        }

        var rules = new LinkedHashSet<IpSubnetFilterRule>();
        var ruleType = IpFilterRuleType.ACCEPT;
        var ruleDefinitions = List.of(COMMA_SPLIT_PATTERN.split(filterRules));

        for (var rule : ruleDefinitions) {
            var ipAndCidrPrefix = SLASH_SPLIT_PATTERN.split(rule);
            var ip = ipAndCidrPrefix[0];
            var cidrPrefix = Integer.parseInt(ipAndCidrPrefix[1]);
            rules.add(new IpSubnetFilterRule(ip, cidrPrefix, ruleType));
        }

        var ruleDescription = adminPath + " " + ruleType + " from " + String.join(",", ruleDefinitions);
        var pathIpFilterRules = new PathIpFilterRules(ruleDescription, adminPath, Set.copyOf(rules));
        log.infof("Created Security Filter rules for %s", pathIpFilterRules);
        return pathIpFilterRules;
    }

    private String makeContextPath(String contextPath, String subPath) {
        if (contextPath.endsWith("/")) {
            return contextPath + subPath;
        }
        return contextPath + "/" + subPath;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {

        if (adminPathIpFilterRules == null) {
            return;
        }

        var requestUri = requestContext.getUriInfo().getRequestUri();
        log.tracef("Processing request: %s", requestUri);

        var requestPath = requestUri.getPath();
        if (requestPath.startsWith(adminPathIpFilterRules.getPathPrefix())) {
            if (!isAdminRequestAllowed()) {
                throw FORBIDDEN_EXCEPTION;
            }
        }
    }

    private boolean isAdminRequestAllowed() {

        var remoteIp = httpServerRequest.connection().remoteAddress();
        var address = new InetSocketAddress(remoteIp.host(), remoteIp.port());
        for (var filterRule : adminPathIpFilterRules.getIpFilterRules()) {
            if (filterRule.matches(address)) {
                return true;
            }
        }

        return false;
    }

    @Data
    static class PathIpFilterRules {

        private final String ruleDescription;

        private final String pathPrefix;

        private final Set<IpSubnetFilterRule> ipFilterRules;

        public String toString() {
            return ruleDescription;
        }
    }
}
