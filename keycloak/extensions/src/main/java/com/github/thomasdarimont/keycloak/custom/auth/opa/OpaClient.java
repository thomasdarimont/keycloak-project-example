package com.github.thomasdarimont.keycloak.custom.auth.opa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.thomasdarimont.keycloak.custom.config.ClientConfig;
import com.github.thomasdarimont.keycloak.custom.config.RealmConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.broker.provider.util.SimpleHttp;
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
import java.util.stream.Collectors;

@JBossLog
public class OpaClient {

    public static final String OPA_ACTION_LOGIN = "login";

    public static final String DEFAULT_OPA_AUTHZ_URL = "http://acme-opa:8181/v1/data/iam/keycloak/allow";

    public static final String OPA_USE_REALM_ROLES = "useRealmRoles";

    public static final String OPA_USE_CLIENT_ROLES = "useClientRoles";

    public static final String OPA_USE_USER_ATTRIBUTES = "useUserAttributes";

    public static final String OPA_USER_ATTRIBUTES = "userAttributes";

    public static final String OPA_USE_REALM_ATTRIBUTES = "useRealmAttributes";

    public static final String OPA_REALM_ATTRIBUTES = "realmAttributes";

    public static final String OPA_USE_CLIENT_ATTRIBUTES = "useClientAttributes";

    public static final String OPA_CLIENT_ATTRIBUTES = "clientAttributes";

    public static final String OPA_USE_GROUPS = "useGroups";

    public static final String OPA_AUTHZ_URL = "authzUrl";

    public AccessResponse checkAccess(KeycloakSession session, Map<String, String> config, RealmModel realm, UserModel user, ClientModel client, String action) {

        var username = user.getUsername();
        var realmRoles = getBoolean(config, OPA_USE_REALM_ROLES, true) ? fetchRealmRoles(user) : null;
        var clientRoles = getBoolean(config, OPA_USE_CLIENT_ROLES, true) ? fetchClientRoles(user, client) : null;
        var userAttributes = getBoolean(config, OPA_USE_USER_ATTRIBUTES, true) ? extractUserAttributes(user, config) : null;
        var groups = getBoolean(config, OPA_USE_GROUPS, true) ? fetchGroupNames(user) : null;

        var subject = new Subject(username, realmRoles, clientRoles, userAttributes, groups);

        var realmAttributes = getBoolean(config, OPA_USE_REALM_ATTRIBUTES, false) ? extractRealmAttributes(realm, config) : null;
        var clientAttributes = getBoolean(config, OPA_USE_CLIENT_ATTRIBUTES, false) ? extractClientAttributes(client, config) : null;
        var resource = new Resource(realm.getName(), realmAttributes, client.getClientId(), clientAttributes);
        var accessRequest = new AccessRequest(subject, resource, action);

        try {
            log.infof("Sending OPA authorization request. realm=%s user=%s client=%s\n%s", //
                    realm.getName(), user.getUsername(), client.getClientId(), JsonSerialization.writeValueAsPrettyString(accessRequest));
        } catch (IOException ignore) {
        }

        var authzUrl = getAuthzUrl(config);
        var http = SimpleHttp.doPost(authzUrl, session);
        http.json(Map.of("input", accessRequest));

        var accessResponse = fetchResponse(http);

        try {
            log.infof("Received OPA authorization response. realm=%s user=%s client=%s\n%s", //
                    realm.getName(), user.getUsername(), client.getClientId(), JsonSerialization.writeValueAsPrettyString(accessResponse));
        } catch (IOException ignore) {
        }
        return accessResponse;
    }

    private <T> Map<String, Object> extractAttributes(T source, Map<String, String> config, String attributesKey, BiFunction<T, String, Object> valueExtractor, Function<T, Map<String, Object>> defaultValuesExtractor) {

        if (config == null) {
            return defaultValuesExtractor.apply(source);
        }

        var requestedAttributes = config.get(attributesKey);
        if (requestedAttributes == null || requestedAttributes.isBlank()) {
            return defaultValuesExtractor.apply(source);
        }

        var attributes = new HashMap<String, Object>();
        for (String attribute : requestedAttributes.trim().split(",")) {
            Object value = valueExtractor.apply(source, attribute);
            attributes.put(attribute, value);
        }

        return attributes;
    }

    private Map<String, Object> extractUserAttributes(UserModel user, Map<String, String> config) {

        var userAttributes = extractAttributes(user, config, OPA_USER_ATTRIBUTES, (u, attr) -> {
            Object value;
            switch (attr) {
                case "id":
                    value = user.getId();
                    break;
                case "email":
                    value = user.getEmail();
                    break;
                case "createdTimestamp":
                    value = user.getCreatedTimestamp();
                    break;
                case "lastName":
                    value = user.getLastName();
                    break;
                case "firstName":
                    value = user.getFirstName();
                    break;
                case "federationLink":
                    value = user.getFederationLink();
                    break;
                case "serviceAccountLink":
                    value = user.getServiceAccountClientLink();
                    break;
                default:
                    value = user.getFirstAttribute(attr);
            }

            return value;
        }, this::extractDefaultUserAttributes);
        return userAttributes;
    }

    private Map<String, Object> extractClientAttributes(ClientModel client, Map<String, String> config) {
        var clientConfig = new ClientConfig(client);
        return extractAttributes(client, config, OPA_CLIENT_ATTRIBUTES, (c, attr) -> clientConfig.getValue(attr), c -> null);
    }

    private Map<String, Object> extractRealmAttributes(RealmModel realm, Map<String, String> config) {
        var realmConfig = new RealmConfig(realm);
        return extractAttributes(realm, config, OPA_REALM_ATTRIBUTES, (r, attr) -> realmConfig.getValue(attr), r -> null);
    }

    private static List<String> fetchGroupNames(UserModel user) {
        return user.getGroupsStream().map(GroupModel::getName).collect(Collectors.toList());
    }

    private static List<String> fetchClientRoles(UserModel user, ClientModel client) {
        return RoleUtils.expandCompositeRolesStream(user.getClientRoleMappingsStream(client)) //
                .map(OpaClient::normalizeRoleName) //
                .collect(Collectors.toList());
    }

    private static List<String> fetchRealmRoles(UserModel user) {
        return RoleUtils.expandCompositeRolesStream(user.getRealmRoleMappingsStream()) //
                .map(OpaClient::normalizeRoleName) //
                .collect(Collectors.toList());
    }

    private static String normalizeRoleName(RoleModel role) {
        if (role.isClientRole()) {
            return ((ClientModel) role.getContainer()).getClientId() + ":" + role.getName();
        }
        return role.getName();
    }

    private boolean getBoolean(Map<String, String> config, String key, boolean defaultValue) {

        if (config == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(config.get(key));
    }

    private Map<String, Object> extractDefaultUserAttributes(UserModel user) {
        return Map.of("id", user.getId(), "email", user.getEmail());
    }

    private static AccessResponse fetchResponse(SimpleHttp http) {
        try {
            var response = http.asResponse();
            try {
                return response.asJson(AccessResponse.class);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        } catch (IOException e) {
            log.error("OPA access request failed", e);
            return new AccessResponse(Map.of("allow", false, "hint", Messages.ACCESS_DENIED));
        }
    }

    private String getAuthzUrl(Map<String, String> config) {

        if (config == null) {
            return DEFAULT_OPA_AUTHZ_URL;
        }

        return config.getOrDefault(OPA_AUTHZ_URL, DEFAULT_OPA_AUTHZ_URL);
    }

    @Data
    static class AccessRequest {

        private final Subject subject;
        private final Resource resource;
        private final String action;
    }

    @Data
    static class Subject {

        private final String username;
        private final List<String> realmRoles;
        private final List<String> clientRoles;
        private final Map<String, Object> attributes;
        private final List<String> groups;
    }

    @Data
    static class Resource {

        private final String realm;

        private final Map<String, Object> realmAttributes;

        private final String clientId;

        private final Map<String, Object> clientAttributes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class AccessResponse {

        private Map<String, Object> result;

        @JsonIgnore
        public boolean isAllowed() {
            return result != null && Boolean.parseBoolean(String.valueOf(result.get("allow")));
        }

        public String getHint() {
            if (result == null) {
                return null;
            }
            Object hint = result.get("hint");
            if (!(hint instanceof String)) {
                return null;
            }
            return (String) hint;
        }
    }

}
