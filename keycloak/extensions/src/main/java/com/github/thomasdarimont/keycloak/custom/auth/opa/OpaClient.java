package com.github.thomasdarimont.keycloak.custom.auth.opa;

import com.github.thomasdarimont.keycloak.custom.config.ClientConfig;
import com.github.thomasdarimont.keycloak.custom.config.ConfigAccessor;
import com.github.thomasdarimont.keycloak.custom.config.RealmConfig;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JBossLog
public class OpaClient {

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    public static final String OPA_ACTION_LOGIN = "login";

    public static final String OPA_ACTION_CHECK_ACCESS = "check_access";

    public static final String DEFAULT_OPA_AUTHZ_URL = "http://acme-opa:8181/v1/data/iam/keycloak/allow";

    public static final String OPA_USE_REALM_ROLES = "useRealmRoles";

    public static final String OPA_USE_CLIENT_ROLES = "useClientRoles";

    public static final String OPA_USER_ATTRIBUTES = "userAttributes";

    public static final String OPA_CONTEXT_ATTRIBUTES = "contextAttributes";

    public static final String OPA_REALM_ATTRIBUTES = "realmAttributes";

    public static final String OPA_CLIENT_ATTRIBUTES = "clientAttributes";

    public static final String OPA_REQUEST_HEADERS = "requestHeaders";

    public static final String OPA_USE_GROUPS = "useGroups";

    public static final String OPA_AUTHZ_URL = "authzUrl";

    public OpaAccessResponse checkAccess(KeycloakSession session, ConfigAccessor config, RealmModel realm, UserModel user, ClientModel client, String action) {

        var username = user.getUsername();
        var realmRoles = config.getBoolean(OPA_USE_REALM_ROLES, true) ? fetchRealmRoles(user) : null;
        var clientRoles = config.getBoolean(OPA_USE_CLIENT_ROLES, true) ? fetchClientRoles(user, client) : null;
        var userAttributes = config.isConfigured(OPA_USER_ATTRIBUTES, true) ? extractUserAttributes(user, config) : null;
        var groups = config.getBoolean(OPA_USE_GROUPS, true) ? fetchGroupNames(user) : null;

        var subject = new Subject(username, realmRoles, clientRoles, userAttributes, groups);

        var realmAttributes = config.isConfigured(OPA_REALM_ATTRIBUTES, false) ? extractRealmAttributes(realm, config) : null;
        var clientAttributes = config.isConfigured(OPA_CLIENT_ATTRIBUTES, false) ? extractClientAttributes(client, config) : null;
        var resource = new Resource(realm.getName(), realmAttributes, client.getClientId(), clientAttributes);
        var accessContext = createAccessContext(session, config, user);

        var accessRequest = new AccessRequest(subject, resource, accessContext, action);

        try {
            log.infof("Sending OPA check access request. realm=%s user=%s client=%s\n%s", //
                    realm.getName(), user.getUsername(), client.getClientId(), JsonSerialization.writeValueAsPrettyString(accessRequest));
        } catch (IOException ioe) {
            log.warn("Failed to prepare check access request", ioe);
        }

        var authzUrl = config.getString(OPA_AUTHZ_URL, DEFAULT_OPA_AUTHZ_URL);
        var http = SimpleHttp.doPost(authzUrl, session);
        http.json(Map.of("input", accessRequest));

        var accessResponse = fetchResponse(http);

        try {
            log.infof("Received OPA authorization response. realm=%s user=%s client=%s\n%s", //
                    realm.getName(), user.getUsername(), client.getClientId(), JsonSerialization.writeValueAsPrettyString(accessResponse));
        } catch (IOException ioe) {
            log.warn("Failed to process received check access response", ioe);
        }
        return accessResponse;
    }

    private AccessContext createAccessContext(KeycloakSession session, ConfigAccessor config, UserModel user) {
        var contextAttributes = config.isConfigured(OPA_CONTEXT_ATTRIBUTES, false) ? extractContextAttributes(session, user, config) : null;
        var headers = config.isConfigured(OPA_REQUEST_HEADERS, false) ? extractRequestHeaders(session, config) : null;
        return new AccessContext(contextAttributes, headers);
    }

    private Map<String, Object> extractRequestHeaders(KeycloakSession session, ConfigAccessor config) {

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

    private Map<String, Object> extractContextAttributes(KeycloakSession session, UserModel user, ConfigAccessor config) {
        var contextAttributes = extractAttributes(user, config, OPA_CONTEXT_ATTRIBUTES, (u, attr) -> {
            Object value;
            switch (attr) {
                case "remoteAddress":
                    value = session.getContext().getConnection().getRemoteAddr();
                    break;
                default:
                    value = null;
            }

            return value;
        }, u -> null);
        return contextAttributes;
    }

    private <T> Map<String, Object> extractAttributes(T source, ConfigAccessor config, String attributesKey, BiFunction<T, String, Object> valueExtractor, Function<T, Map<String, Object>> defaultValuesExtractor) {

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

    private Map<String, Object> extractUserAttributes(UserModel user, ConfigAccessor config) {

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

    private Map<String, Object> extractClientAttributes(ClientModel client, ConfigAccessor config) {
        var clientConfig = new ClientConfig(client);
        return extractAttributes(client, config, OPA_CLIENT_ATTRIBUTES, (c, attr) -> clientConfig.getValue(attr), c -> null);
    }

    private Map<String, Object> extractRealmAttributes(RealmModel realm, ConfigAccessor config) {
        var realmConfig = new RealmConfig(realm);
        return extractAttributes(realm, config, OPA_REALM_ATTRIBUTES, (r, attr) -> realmConfig.getValue(attr), r -> null);
    }

    private static List<String> fetchGroupNames(UserModel user) {
        return user.getGroupsStream().map(GroupModel::getName).collect(Collectors.toList());
    }

    private static List<String> fetchClientRoles(UserModel user, ClientModel client) {
        Stream<RoleModel> explicitClientRoles = RoleUtils.expandCompositeRolesStream(user.getClientRoleMappingsStream(client));
        Stream<RoleModel> implicitClientRoles = RoleUtils.expandCompositeRolesStream(user.getRealmRoleMappingsStream());
        return Stream.concat(explicitClientRoles, implicitClientRoles) //
                .filter(RoleModel::isClientRole) //
                .map(OpaClient::normalizeRoleName) //
                .collect(Collectors.toList());
    }

    private static List<String> fetchRealmRoles(UserModel user) {
        return RoleUtils.expandCompositeRolesStream(user.getRealmRoleMappingsStream()) //
                .filter(r -> !r.isClientRole()).map(OpaClient::normalizeRoleName) //
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

    private static OpaAccessResponse fetchResponse(SimpleHttp http) {
        try {
            try (var response = http.asResponse()) {
                return response.asJson(OpaAccessResponse.class);
            }
        } catch (IOException e) {
            log.error("OPA access request failed", e);
            return new OpaAccessResponse(Map.of("allow", false, "hint", Messages.ACCESS_DENIED));
        }
    }

    record AccessRequest(Subject subject, Resource resource, AccessContext context, String action) {
    }

    record Subject(String username, //
                   List<String> realmRoles, //
                   List<String> clientRoles, //
                   Map<String, Object> attributes, //
                   List<String> groups) {
    }

    record Resource(String realm, //
                    Map<String, Object> realmAttributes, //
                    String clientId, //
                    Map<String, Object> clientAttributes) {

    }

    record AccessContext(Map<String, Object> attributes, Map<String, Object> headers) {
    }

}
