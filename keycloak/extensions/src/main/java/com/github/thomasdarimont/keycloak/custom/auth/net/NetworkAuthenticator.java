package com.github.thomasdarimont.keycloak.custom.auth.net;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.messages.Messages;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@link Authenticator} that can check the remote IP address of the incoming request against a list of allowed networks.
 * <p>
 * The list of allowed networks can be configured via the AuthenticatorConfig or via a client attribute.
 * <p>
 * <p>
 * This authenticator can be used in the following contexts
 * <ul>
 * <li>Browser Flow</li>
 * <li>Direct Grant Flow</li>
 * </ul>
 */
@JBossLog
public class NetworkAuthenticator implements Authenticator {

    static final NetworkAuthenticator INSTANCE = new NetworkAuthenticator();

    public static final String PROVIDER_ID = "acme-network-authenticator";

    public static final String REMOTE_IP_HEADER_PROPERTY = "remoteIpHeader";

    public static final String ALLOWED_NETWORKS_PROPERTY = "allowedNetworks";

    public static final String X_FORWARDED_FOR = "X-Forwarded-For";

    public static final String ACME_ALLOWED_NETWORKS_CLIENT_ATTRIBUTE = "acmeAllowedNetworks";

    /**
     * Authenticates within Browser and Direct Grant flow authentication flows.
     *
     * @param context
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {

        var realm = context.getRealm();
        var authSession = context.getAuthenticationSession();
        var client = authSession.getClient();

        var allowedNetworks = resolveAllowedNetworks(context.getAuthenticatorConfig(), client);
        if (allowedNetworks == null) {
            // skip check since we don't have any network restrictions configured
            log.debugf("Skip check for source IP based on network. realm=%s, client=%s", //
                    realm.getName(), client.getClientId());
            context.success();
            return;
        }

        var remoteIp = resolveRemoteIp( //
                context.getAuthenticatorConfig(), //
                context.getHttpRequest(), //
                context.getConnection().getRemoteAddr() //
        );
        if (remoteIp == null) {
            context.attempted();
            log.warnf("Could not determine remoteIp, step marked as attempted. realm=%s, client=%s", //
                    realm.getName(), client.getClientId());
            return;
        }

        var ipAllowed = isAccessAllowed(allowedNetworks, remoteIp, realm, client);
        if (ipAllowed) {
            log.debugf("Allowed source IP based on allowed networks. realm=%s, client=%s, IP=%s", //
                    realm.getName(), client.getClientId(), remoteIp);
            context.success();
            return;
        }

        log.debugf("Rejected source IP based on allowed networks. realm=%s, client=%s, IP=%s", //
                realm.getName(), client.getClientId(), remoteIp);

        var challengeResponse = errorResponse(context, Response.Status.UNAUTHORIZED.getStatusCode(), "invalid_request", "Access denied", authSession.getAuthNote("auth_type"));
        context.failure(AuthenticationFlowError.ACCESS_DENIED, challengeResponse);
    }


    @VisibleForTesting
    boolean isAccessAllowed(String allowedNetworks, String remoteIp, RealmModel realm, ClientModel client) {

        var ipAllowed = false;
        for (String allowedNetwork : allowedNetworks.split(",")) {
            ipAllowed = isRemoteIpAllowed(allowedNetwork, remoteIp);
            if (ipAllowed) {
                log.debugf("Matched source IP based on allowed network. realm=%s, client=%s, IP=%s, network=%s", //
                        realm.getName(), client.getClientId(), remoteIp, allowedNetwork);
                break;
            } else {
                log.tracef("Rejected source IP based on allowed network. realm=%s, client=%s, IP=%s, network=%s", //
                        realm.getName(), client.getClientId(), remoteIp, allowedNetwork);
            }
        }
        return ipAllowed;
    }

    /**
     * Extracts the allowed networks as comma separated String from the AuthenticatorConfig or the client attribute.
     *
     * @param config
     * @param client
     * @return
     */
    @VisibleForTesting
    String resolveAllowedNetworks(AuthenticatorConfigModel config, ClientModel client) {

        var allowedNetworks = getAllowedNetworksForClient(client);
        if (isAllowedNetworkConfigured(allowedNetworks)) {
            return allowedNetworks;
        }

        allowedNetworks = getAllowedNetworksForAuthenticator(config);
        if (isAllowedNetworkConfigured(allowedNetworks)) {
            return allowedNetworks;
        }

        return null;
    }

    public Response errorResponse(AuthenticationFlowContext flowContext, int status, String error, String errorDescription, String authType) {

        if ("code".equals(authType)) {
            // auth code implies browser flow, so we need to render a form here
            var form = flowContext.form().setExecution(flowContext.getExecution().getId());
            form.setError(Messages.ACCESS_DENIED);
            return form.createErrorPage(Response.Status.FORBIDDEN);
        }

        // client authentication or direct grant flow
        OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(error, errorDescription);
        return Response.status(status).entity(errorRep).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    private boolean isAllowedNetworkConfigured(String allowedNetworks) {
        return allowedNetworks != null && !allowedNetworks.isBlank();
    }

    @VisibleForTesting
    private String getAllowedNetworksForAuthenticator(AuthenticatorConfigModel authenticatorConfig) {

        if (authenticatorConfig == null) {
            return null;
        }

        var config = authenticatorConfig.getConfig();
        if (config == null) {
            return null;
        }

        return config.get(ALLOWED_NETWORKS_PROPERTY);
    }

    @VisibleForTesting
    String getAllowedNetworksForClient(ClientModel client) {
        return client.getAttribute(ACME_ALLOWED_NETWORKS_CLIENT_ATTRIBUTE);
    }

    @VisibleForTesting
    boolean isRemoteIpAllowed(String allowedNetwork, String remoteIp) {

        boolean allowed = false;

        if (allowedNetwork.contains("/")) {
            /*
             CIDR notation, e.g:
             192.168.178.0/24 - Allow access from a subnet
             192.168.178.10/32 - Allow access from a single IP
             */
            var ipAndCidrRange = allowedNetwork.split("/");
            var ip = ipAndCidrRange[0];
            int cidrRange = Integer.parseInt(ipAndCidrRange[1]);
            var rule = new IpSubnetFilterRule(ip, cidrRange, IpFilterRuleType.ACCEPT);
            allowed = rule.matches(new InetSocketAddress(remoteIp, 1 /* unsed */));
        } else {
            /*
             explicit IP addresses, e.g:
             192.168.178.10 - Allow access from a single IP
             */
            allowed = remoteIp.equals(allowedNetwork.trim());
        }

        return allowed;
    }

    @VisibleForTesting
    String resolveRemoteIp(AuthenticatorConfigModel authenticatorConfig, HttpRequest httpRequest, String remoteAddress) {

        var remoteIpHeaderName = getRemoteIpHeaderName(authenticatorConfig);
        var httpHeaders = httpRequest.getHttpHeaders();
        if (X_FORWARDED_FOR.equals(remoteIpHeaderName)) {
            // see: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For
            // X-Forwarded-For: <client_ip>, <proxy1_ip>, <proxy2_ip>
            String xForwardedForHeaderValue = httpHeaders.getHeaderString(X_FORWARDED_FOR);
            if (xForwardedForHeaderValue != null) {
                String[] ipAddresses = xForwardedForHeaderValue.split(",");
                // take the first IP address
                return ipAddresses[0].trim();
            }
        }

        // TODO add support for Standard Forwarded Header
        var remoteIpFromHeader = httpHeaders.getHeaderString(remoteIpHeaderName);
        if (remoteIpFromHeader != null) {
            return remoteIpFromHeader;
        }

        return remoteAddress;

    }

    @VisibleForTesting
    String getRemoteIpHeaderName(AuthenticatorConfigModel authenticatorConfig) {

        if (authenticatorConfig == null) {
            return X_FORWARDED_FOR;
        }

        Map<String, String> config = authenticatorConfig.getConfig();
        if (config == null) {
            return X_FORWARDED_FOR;
        }

        String remoteIpHeaderName = config.get(REMOTE_IP_HEADER_PROPERTY);
        if (remoteIpHeaderName == null || remoteIpHeaderName.isBlank()) {
            return X_FORWARDED_FOR;
        }

        return remoteIpHeaderName;
    }

    @Override
    public void action(AuthenticationFlowContext flowContext) {
        // NOOP
    }

    @Override
    public boolean requiresUser() {
        // no resolved user needed
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    @AutoService(AuthenticatorFactory.class)
    public static class Factory implements AuthenticatorFactory {

        static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

        static {
            var list = ProviderConfigurationBuilder.create() //
                    .property().name(REMOTE_IP_HEADER_PROPERTY) //
                    .type(ProviderConfigProperty.STRING_TYPE) //
                    .label("Remote IP Header") //
                    .defaultValue(X_FORWARDED_FOR) //
                    .helpText("Header which contains the actual remote IP of a user agent. If empty the remote address will be resolved from the TCP connection. If the headername is X-Forwarded-For the header value is split on ',' and the first values is used as the remote address.") //
                    .add() //

                    .property().name(ALLOWED_NETWORKS_PROPERTY) //
                    .type(ProviderConfigProperty.STRING_TYPE) //
                    .label("Allowed networks") //
                    .defaultValue(null) //
                    .helpText("Comma separated list of allowed networks. This supports CIDR network ranges and single IP adresses. If left empty ALL networks are allowed. Configuration can be overriden via client attribute acmeAllowedNetworks. Examples: 192.168.178.0/24, 192.168.178.12/32, 192.168.178.13") //
                    .add() //

                    .build();

            CONFIG_PROPERTIES = Collections.unmodifiableList(list);
        }


        @Override
        public String getId() {
            return PROVIDER_ID;
        }

        @Override
        public String getDisplayType() {
            return "Acme: Network Authenticator";
        }

        @Override
        public String getReferenceCategory() {
            return "network";
        }

        @Override
        public String getHelpText() {
            return "Controls access by checking the network address of the incoming request.";
        }

        @Override
        public Authenticator create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public void init(Config.Scope scope) {

        }

        @Override
        public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

        }

        @Override
        public boolean isConfigurable() {
            return true;
        }

        @Override
        public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
            return REQUIREMENT_CHOICES;
        }

        @Override
        public boolean isUserSetupAllowed() {
            return false;
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            return CONFIG_PROPERTIES;
        }

        @Override
        public void close() {

        }
    }
}
