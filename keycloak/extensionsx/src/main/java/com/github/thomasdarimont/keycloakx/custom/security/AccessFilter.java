package com.github.thomasdarimont.keycloakx.custom.security;

import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import lombok.Data;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.config.Config;
import org.keycloak.configuration.Configuration;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@JBossLog
@Provider
public class AccessFilter implements ContainerRequestFilter {

    public static final String DEFAULT_IP_FILTER_RULES = "127.0.0.1/24,192.168.80.1/16,172.0.0.1/8";
    public static final String ADMIN_IP_FILTER_RULES_ALLOW = "acme.keycloak.admin.ip-filter-rules.allow";

    private final PathIpFilterRules adminPathIpFilterRules;

    @Context
    private HttpServerRequest httpServerRequest;

    public AccessFilter() {
        Config config = Configuration.getConfig();
        this.adminPathIpFilterRules = createAdminIpFilterRules(config);
        log.infof("Created Security Filter rules for %s", adminPathIpFilterRules);
    }

    private PathIpFilterRules createAdminIpFilterRules(Config config) {

        String contextPath = config.getValue("quarkus.http.root-path", String.class);

        String adminPath = makeContextPath(contextPath, "admin");

        String filterRules = config //
                .getOptionalValue(ADMIN_IP_FILTER_RULES_ALLOW, String.class) //
                .orElse(DEFAULT_IP_FILTER_RULES);

        var rules = new LinkedHashSet<IpSubnetFilterRule>();
        var ruleType = IpFilterRuleType.ACCEPT;
        var ruleDefinitions = List.of(filterRules.split(","));

        for (String rule : ruleDefinitions) {
            String[] tokens = rule.split("/");
            String ip = tokens[0];
            int cidrPrefix = Integer.parseInt(tokens[1]);
            rules.add(new IpSubnetFilterRule(ip, cidrPrefix, ruleType));
        }

        var ruleDescription = adminPath + " " + ruleType + " from " + String.join(",", ruleDefinitions);

        return new PathIpFilterRules(ruleDescription, adminPath, Set.copyOf(rules));
    }

    private String makeContextPath(String contextPath, String subPath) {
        if (contextPath.endsWith("/")) {
            return contextPath + subPath;
        }
        return contextPath + "/" + subPath;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {

        URI requestUri = requestContext.getUriInfo().getRequestUri();
        String requestPath = requestUri.getPath();
        log.tracef("Processing request: %s", requestUri);

        if (requestPath.startsWith(adminPathIpFilterRules.getPathPrefix())) {
            boolean requestAllowed = isAdminRequestAllowed();

            if (!requestAllowed) {
                throw new ForbiddenException();
            }
        }
    }

    private boolean isAdminRequestAllowed() {

        SocketAddress remoteIp = httpServerRequest.connection().remoteAddress();

        InetSocketAddress address = new InetSocketAddress(remoteIp.host(), remoteIp.port());
        for (IpFilterRule filterRule : adminPathIpFilterRules.getIpFilterRules()) {
            if (filterRule.matches(address)) {
                return false;
            }
        }

        return true;
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
