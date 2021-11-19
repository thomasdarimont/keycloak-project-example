package com.github.thomasdarimont.keycloakx.custom.security;

import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.configuration.Configuration;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

@JBossLog
@Provider
public class AccessFilter implements ContainerRequestFilter {

    public static final String DEFAULT_IP_FILTER_RULES = "127.0.0.1/24,192.168.80.1/16,172.0.0.1/8";
    public static final String ADMIN_IP_FILTER_RULES_ALLOW = "acme.keycloak.admin.ip-filter-rules.allow";

    private static final SmallRyeConfig config;

    static {
        config = Configuration.getConfig();
    }

    private final Set<IpFilterRule> ipFilterRules;

    private final String adminPath;

    @Context
    private HttpServerRequest httpServerRequest;

    public AccessFilter() {
        log.info("Initialize Security Filter rules");
        String contextPath = config.getConfigValue("quarkus.http.root-path").getValue();

        this.adminPath = makeContextPath(contextPath, "admin");
        this.ipFilterRules = createIpSubnetFilterRules();
    }

    private Set<IpFilterRule> createIpSubnetFilterRules() {

        String ipFilterRulesString = config.getConfigValue(ADMIN_IP_FILTER_RULES_ALLOW).getValue();

        if (ipFilterRulesString == null) {
            ipFilterRulesString = DEFAULT_IP_FILTER_RULES;
        }

        var rules = new LinkedHashSet<IpSubnetFilterRule>();
        for (String rule : ipFilterRulesString.split(",")) {
            String[] tokens = rule.split("/");
            String ip = tokens[0];
            int cidrPrefix = Integer.parseInt(tokens[1]);
            rules.add(new IpSubnetFilterRule(ip, cidrPrefix, IpFilterRuleType.ACCEPT));
        }

        return Set.copyOf(rules);
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

        if (requestPath.startsWith(adminPath)) {
            boolean requestAllowed = isRequestAllowed();

            if (!requestAllowed) {
                throw new ForbiddenException();
            }
        }
    }

    private boolean isRequestAllowed() {

        SocketAddress remoteIp = httpServerRequest.connection().remoteAddress();

        boolean requestAllowed = false;
        InetSocketAddress address = new InetSocketAddress(remoteIp.host(), remoteIp.port());
        for (IpFilterRule filterRule : ipFilterRules) {
            if (filterRule.matches(address)) {
                requestAllowed = true;
                break;
            }
        }

        return requestAllowed;
    }
}
