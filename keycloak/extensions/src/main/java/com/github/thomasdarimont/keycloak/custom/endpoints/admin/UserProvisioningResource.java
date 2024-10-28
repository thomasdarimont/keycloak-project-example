package com.github.thomasdarimont.keycloak.custom.endpoints.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.Data;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.UserCache;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Custom Admin Resource for User Provisioning
 */
@JBossLog
public class UserProvisioningResource {

    private final KeycloakSession session;

    private final RealmModel realm;

    private final AdminPermissionEvaluator auth;

    private final AdminEventBuilder adminEvent;

    private final UserProvisioningConfig privisioningConfig;

    public UserProvisioningResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent, UserProvisioningConfig privisioningConfig) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
        this.privisioningConfig = privisioningConfig;
    }

    /**
     * Supported Operations
     * - Manage User Attributes
     *
     * https://id.acme.test:8443/auth/admin/realms/acme-workshop/custom-admin-resources/users/provisioning
     *
     * @return
     */
    @Path("/provisioning")
    @POST
    public Response provisionUsers(UserProvisioningRequest provisioningRequest) {

        if (!isAuthorized()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        if (provisioningRequest.getUsers() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Instant startedAt = Instant.now();

        Map<String, Object> adminOperationRep = new LinkedHashMap<>();
        adminOperationRep.put("startedAt", startedAt.toString());
        try {
            RealmModel realm = session.getContext().getRealm();
            UserProvider userProvider = session.getProvider(UserProvider.class);

            var provisioningContext = new UserProvisioningContext(realm, userProvider, provisioningRequest);

            for (UserRepresentation userRep : provisioningRequest.getUsers()) {
                var result = new UserProvisioningResult();
                try {
                    result.setUsername(userRep.getUsername());

                    provisionUser(result, userRep, provisioningContext);

                    if (result.getError() != null) {
                        log.debugf("Error during user provisioning. realm=%s username=%s error=%s",
                                realm.getName(), result.getUsername(), result.getError());
                    }
                } catch (Exception ex) {
                    result.setStatus(UserProvisioningStatus.ERROR);
                    result.setError(UserProvisioningError.UNKNOWN);
                    result.setErrorDetails(ex.getMessage());
                }
                provisioningContext.addResult(result);
            }

            var response = createProvisioningResponse(provisioningContext);

            var errors = response.getErrors();
            var updated = response.getUpdated();
            adminOperationRep.put("errors", errors == null ? 0 : errors.size());
            adminOperationRep.put("updates", updated == null ? 0 : updated.size());

            return Response.ok().entity(response).build();
        } finally {
            adminEvent.operation(OperationType.UPDATE)
                    .resourcePath(session.getContext().getUri())
                    .representation(adminOperationRep)
                    .success();
        }
    }

    private boolean isAuthorized() {
        String requiredRealmRole = privisioningConfig.getRequiredRealmRole();

        // ensure access token originated from master realm
        if (!"master".equals(auth.adminAuth().getRealm().getName())) {
            return false;
        }

        // ensure user has required realm role in the master realm
        return auth.adminAuth().hasRealmRole(requiredRealmRole);
    }

    private UserProvisioningResponse createProvisioningResponse(UserProvisioningContext provisioningContext) {

        // TreeSet for stable error order in map
        Map<UserProvisioningError, Set<String>> errors = new TreeMap<>();

        Set<String> updated = new LinkedHashSet<>();
        List<UserProvisioningResult> results = provisioningContext.getResults();
        for (var result : results) {
            if (result.getError() != null) {
                errors.computeIfAbsent(result.getError(), error -> new LinkedHashSet<>()).add(result.getUsername());
            } else {
                updated.add(result.getUsername());
            }
        }

        return new UserProvisioningResponse(updated, errors);
    }

    private void provisionUser(UserProvisioningResult result, UserRepresentation userRep, UserProvisioningContext provisioningContext) {

        if (userRep.getUsername() == null) {
            result.setUserId(userRep.getId());
            result.setEmail(userRep.getEmail());
            result.setStatus(UserProvisioningStatus.ERROR);
            result.setError(UserProvisioningError.INVALID_INPUT);
            result.setErrorDetails("Missing username in request!");
            return;
        }

        UserProvider userProvider = provisioningContext.getUserProvider();
        RealmModel realm = provisioningContext.getRealm();
        UserModel user = userProvider.getUserByUsername(realm, userRep.getUsername());
        if (user == null) {
            result.setUserId(userRep.getId());
            result.setEmail(userRep.getEmail());
            result.setUsername(userRep.getUsername());
            result.setStatus(UserProvisioningStatus.ERROR);
            result.setError(UserProvisioningError.USER_NOT_FOUND);
            return;
        }

        UserProvisioningStatus attributeUpdateResult = updateUserAttributes(userRep, user);

        if (UserProvisioningStatus.UPDATED.equals(attributeUpdateResult)) {
            // ensure user is removed from cache to make update visible
            UserCache userCache = session.getProvider(UserCache.class);
            userCache.evict(realm, user);

            UserRepresentation updatedUserRep = new UserRepresentation();
            updatedUserRep.setId(user.getId());
            updatedUserRep.setUsername(user.getUsername());
            updatedUserRep.setAttributes(userRep.getAttributes());

            // generate admin event for the provisioning
            adminEvent.operation(OperationType.UPDATE)
                    .resourcePath(session.getContext().getUri(), updatedUserRep.getId())
                    .representation(updatedUserRep).success();
        }

        result.setUserId(user.getId());
        result.setUsername(user.getUsername());
        result.setEmail(user.getEmail());
        result.setStatus(attributeUpdateResult);
    }

    private UserProvisioningStatus updateUserAttributes(UserRepresentation userRep, UserModel user) {

        Map<String, List<String>> userRepAttributes = userRep.getAttributes();
        if (userRepAttributes == null) {
            // no attribute updates given, skip attribute update
            return UserProvisioningStatus.SKIPPED;
        }

        // Handle managed attributes only
        Pattern managedAttributePattern = privisioningConfig.getManagedAttributePattern();
        var managedAttributes = userRepAttributes.entrySet().stream()
                .filter(attribute -> managedAttributePattern.matcher(attribute.getKey()).matches())
                .collect(Collectors.toList());

        if (managedAttributes.isEmpty()) {
            // empty attributes given -> remove all managed attributes
            for (var attributeName : user.getAttributes().keySet()) {
                if (managedAttributePattern.matcher(attributeName).matches()) {
                    user.removeAttribute(attributeName);
                }
            }
            return UserProvisioningStatus.UPDATED;
        }

        // update requested attributes
        // attributes with null values will be removed!
        for (var entry : userRepAttributes.entrySet()) {
            if (entry.getValue() != null) {
                // update value
                user.setAttribute(entry.getKey(), entry.getValue());
            } else {
                // remove value
                user.removeAttribute(entry.getKey());
            }
        }
        return UserProvisioningStatus.UPDATED;
    }

    @Data
    public static class UserProvisioningRequest {

        List<UserRepresentation> users;
    }

    @Data
    public static class UserProvisioningContext {

        private final RealmModel realm;

        private final UserProvider userProvider;

        private final UserProvisioningRequest importRequest;

        private List<UserProvisioningResult> results;

        private boolean errorFound;

        public UserProvisioningContext(RealmModel realm, UserProvider userProvider, UserProvisioningRequest importRequest) {
            this.realm = realm;
            this.userProvider = userProvider;
            this.importRequest = importRequest;
            this.results = new ArrayList<>();
        }

        public void addResult(UserProvisioningResult result) {
            if (result.getError() != null) {
                errorFound = true;
            }
            this.results.add(result);
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class UserProvisioningResponse {
        final Set<String> updated;
        final Map<UserProvisioningError, Set<String>> errors;
    }

    public static enum UserProvisioningStatus {
        UPDATED, SKIPPED, ERROR
    }

    public static enum UserProvisioningError {
        INVALID_INPUT, USER_NOT_FOUND, UNKNOWN
    }

    @Data
    public static class UserProvisioningResult {

        String userId;

        String username;

        String email;

        UserProvisioningStatus status;

        UserProvisioningError error;

        String errorDetails;
    }

    @Data
    public static class UserProvisioningConfig {

        private final String requiredRealmRole;

        private final Pattern managedAttributePattern;
    }

}
