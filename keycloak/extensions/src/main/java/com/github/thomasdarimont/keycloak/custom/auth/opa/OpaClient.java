package com.github.thomasdarimont.keycloak.custom.auth.opa;

import com.github.thomasdarimont.keycloak.custom.auth.authzen.AuthZen;
import com.github.thomasdarimont.keycloak.custom.config.ClientConfig;
import com.github.thomasdarimont.keycloak.custom.config.ConfigAccessor;
import com.github.thomasdarimont.keycloak.custom.config.RealmConfig;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.services.messages.Messages;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JBossLog
public class OpaClient {

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    public static final String OPA_ACTION_LOGIN = "login";

    public static final String OPA_ACTION_CHECK_ACCESS = "check_access";

    public static final String DEFAULT_OPA_AUTHZ_URL = "http://acme-opa:8181/v1/data/iam/keycloak/allow";

    public static final String OPA_ACTION = "action";

    public static final String OPA_DESCRIPTION = "description";

    public static final String OPA_RESOURCE_TYPE = "resource_type";

    public static final String OPA_RESOURCE_CLAIM_NAME = "resource_claim_name";

    public static final String OPA_USE_REALM_ROLES = "useRealmRoles";

    public static final String OPA_USE_CLIENT_ROLES = "useClientRoles";

    public static final String OPA_USER_ATTRIBUTES = "userAttributes";

    public static final String OPA_CONTEXT_ATTRIBUTES = "contextAttributes";

    public static final String OPA_REALM_ATTRIBUTES = "realmAttributes";

    public static final String OPA_CLIENT_ATTRIBUTES = "clientAttributes";

    public static final String OPA_REQUEST_HEADERS = "requestHeaders";

    public static final String OPA_USE_GROUPS = "useGroups";

    public static final String OPA_AUTHZ_URL = "authzUrl";

    public OpaAccessResponse checkAccess(KeycloakSession session, ConfigAccessor config, RealmModel realm, UserModel user, ClientModel client, String actionName) {
        var resource = createResource(config, realm, client);
        return checkAccess(session, config, realm, user, client, actionName, resource);
    }

    public OpaAccessResponse checkAccess(KeycloakSession session, ConfigAccessor config, RealmModel realm, UserModel user, ClientModel client, String actionName, AuthZen.Resource resource) {

        var subject = createSubject(config, user, client);
        var accessContext = createAccessContext(session, config, user);
        var action = new AuthZen.Action(actionName);
        var accessRequest = new AuthZen.AccessRequest(subject, action, resource, accessContext);

        try {
            log.infof("Sending OPA check access request. realm=%s user=%s client=%s actionName=%s resource=%s\n%s", //
                    realm.getName(), user.getUsername(), client.getClientId(), actionName, resource, JsonSerialization.writeValueAsPrettyString(accessRequest));
        } catch (IOException ioe) {
            log.warn("Failed to prepare check access request", ioe);
        }

        var authzUrl = config.getString(OPA_AUTHZ_URL, DEFAULT_OPA_AUTHZ_URL);
        var request = SimpleHttp.doPost(authzUrl, session);
        request.json(Map.of("input", accessRequest));

        var accessResponse = fetchResponse(request);

        try {
            log.infof("Received OPA authorization response. realm=%s user=%s client=%s\n%s", //
                    realm.getName(), user.getUsername(), client.getClientId(), JsonSerialization.writeValueAsPrettyString(accessResponse));
        } catch (IOException ioe) {
            log.warn("Failed to process received check access response", ioe);
        }
        return accessResponse;
    }

    protected AuthZen.Subject createSubject(ConfigAccessor config, UserModel user, ClientModel client) {
        var username = user.getUsername();
        var realmRoles = config.getBoolean(OPA_USE_REALM_ROLES, true) ? fetchRealmRoles(user) : null;
        var clientRoles = config.getBoolean(OPA_USE_CLIENT_ROLES, true) ? fetchClientRoles(user, client) : null;
        var userAttributes = config.isConfigured(OPA_USER_ATTRIBUTES, true) ? extractUserAttributes(user, config) : null;
        var groups = config.getBoolean(OPA_USE_GROUPS, true) ? fetchGroupNames(user) : null;

        var properties = new HashMap<String, Object>();
        if (CollectionUtil.isNotEmpty(realmRoles)) {
            properties.put("realmRoles", realmRoles);
        }
        if (CollectionUtil.isNotEmpty(clientRoles)) {
            properties.put("clientRoles", clientRoles);
        }
        if (userAttributes != null && !userAttributes.isEmpty()) {
            properties.put("userAttributes", userAttributes);
        }
        if (CollectionUtil.isNotEmpty(groups)) {
            properties.put("groups", groups);
        }
        return new AuthZen.Subject("user", username, properties);
    }

    protected AuthZen.Resource createResource(ConfigAccessor config, RealmModel realm, ClientModel client) {
        var realmAttributes = config.isConfigured(OPA_REALM_ATTRIBUTES, false) ? extractRealmAttributes(realm, config) : null;
        var clientAttributes = config.isConfigured(OPA_CLIENT_ATTRIBUTES, false) ? extractClientAttributes(client, config) : null;
        var properties = new HashMap<String, Object>();
        properties.put("realmAttributes", realmAttributes);
        properties.put("clientAttributes", clientAttributes);
        properties.put("clientId", client.getClientId());
        return new AuthZen.Resource("realm", realm.getName(), properties);
    }

    protected Map<String, Object> createAccessContext(KeycloakSession session, ConfigAccessor config, UserModel user) {
        var contextAttributes = config.isConfigured(OPA_CONTEXT_ATTRIBUTES, false) ? extractContextAttributes(session, user, config) : null;
        var headers = config.isConfigured(OPA_REQUEST_HEADERS, false) ? extractRequestHeaders(session, config) : null;
        Map<String, Object> accessContext = new HashMap<>();
        accessContext.put("contextAttributes", contextAttributes);
        accessContext.put("headers", headers);
        return accessContext;
    }

    protected Map<String, Object> extractRequestHeaders(KeycloakSession session, ConfigAccessor config) {

        var headerNames = config.getValue(OPA_REQUEST_HEADERS);
        if (headerNames == null || headerNames.isBlank()) {
            return null;
        }

        var requestHeaders = session.getContext().getRequestHeaders();
        var headers = new HashMap<String, Object>();
        for (String header : COMMA_PATTERN.split(headerNames.trim())) {
            var value = requestHeaders.getHeaderString(header);
            headers.put(header, value);
        }

        if (headers.isEmpty()) {
            return null;
        }

        return headers;
    }

    protected Map<String, Object> extractContextAttributes(KeycloakSession session, UserModel user, ConfigAccessor config) {
        var contextAttributes = extractAttributes(user, config, OPA_CONTEXT_ATTRIBUTES, (u, attr) -> {
            Object value = switch (attr) {
                case "remoteAddress" -> session.getContext().getConnection().getRemoteAddr();
                default -> null;
            };

            return value;
        }, u -> null);
        return contextAttributes;
    }

    protected <T> Map<String, Object> extractAttributes(T source, ConfigAccessor config, String attributesKey, BiFunction<T, String, Object> valueExtractor, Function<T, Map<String, Object>> defaultValuesExtractor) {

        if (config == null) {
            return defaultValuesExtractor.apply(source);
        }

        var requestedAttributes = config.getValue(attributesKey);
        if (requestedAttributes == null || requestedAttributes.isBlank()) {
            return defaultValuesExtractor.apply(source);
        }

        var attributes = new HashMap<String, Object>();
        for (String attribute : COMMA_PATTERN.split(requestedAttributes.trim())) {
            Object value = valueExtractor.apply(source, attribute);
            attributes.put(attribute, value);
        }

        return attributes;
    }

    protected Map<String, Object> extractUserAttributes(UserModel user, ConfigAccessor config) {

        var userAttributes = extractAttributes(user, config, OPA_USER_ATTRIBUTES, (u, attr) -> {
            Object value = switch (attr) {
                case "id" -> user.getId();
                case "email" -> user.getEmail();
                case "createdTimestamp" -> user.getCreatedTimestamp();
                case "lastName" -> user.getLastName();
                case "firstName" -> user.getFirstName();
                case "federationLink" -> user.getFederationLink();
                case "serviceAccountLink" -> user.getServiceAccountClientLink();
                default -> user.getFirstAttribute(attr);
            };

            return value;
        }, this::extractDefaultUserAttributes);
        return userAttributes;
    }

    protected Map<String, Object> extractClientAttributes(ClientModel client, ConfigAccessor config) {
        var clientConfig = new ClientConfig(client);
        return extractAttributes(client, config, OPA_CLIENT_ATTRIBUTES, (c, attr) -> clientConfig.getValue(attr), c -> null);
    }

    protected Map<String, Object> extractRealmAttributes(RealmModel realm, ConfigAccessor config) {
        var realmConfig = new RealmConfig(realm);
        return extractAttributes(realm, config, OPA_REALM_ATTRIBUTES, (r, attr) -> realmConfig.getValue(attr), r -> null);
    }

    protected List<String> fetchGroupNames(UserModel user) {
        return user.getGroupsStream().map(GroupModel::getName).collect(Collectors.toList());
    }

    protected List<String> fetchClientRoles(UserModel user, ClientModel client) {
        Stream<RoleModel> explicitClientRoles = RoleUtils.expandCompositeRolesStream(user.getClientRoleMappingsStream(client));
        Stream<RoleModel> implicitClientRoles = RoleUtils.expandCompositeRolesStream(user.getRealmRoleMappingsStream());
        return Stream.concat(explicitClientRoles, implicitClientRoles) //
                .filter(RoleModel::isClientRole) //
                .map(this::normalizeRoleName) //
                .collect(Collectors.toList());
    }

    protected List<String> fetchRealmRoles(UserModel user) {
        // Set<RoleModel> xxx = RoleUtils.getDeepUserRoleMappings(user);
        return RoleUtils.expandCompositeRolesStream(user.getRealmRoleMappingsStream()) //
                .filter(r -> !r.isClientRole()).map(this::normalizeRoleName) //
                .collect(Collectors.toList());
    }

    protected String normalizeRoleName(RoleModel role) {
        if (role.isClientRole()) {
            return ((ClientModel) role.getContainer()).getClientId() + ":" + role.getName();
        }
        return role.getName();
    }

    protected boolean getBoolean(Map<String, String> config, String key, boolean defaultValue) {

        if (config == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(config.get(key));
    }

    protected Map<String, Object> extractDefaultUserAttributes(UserModel user) {
        return Map.of("id", user.getId(), "email", user.getEmail());
    }

    protected OpaAccessResponse fetchResponse(SimpleHttp request) {
        try {
            log.debugf("Fetching url=%s", request.getUrl());

            try (var response = request.asResponse()) {
                return response.asJson(OpaAccessResponse.class);
            }
        } catch (IOException e) {
            log.error("OPA access request failed", e);
            return new OpaAccessResponse(new AuthZen.AccessResponse(false, Map.of("hint", Messages.ACCESS_DENIED)));
        }
    }

}
