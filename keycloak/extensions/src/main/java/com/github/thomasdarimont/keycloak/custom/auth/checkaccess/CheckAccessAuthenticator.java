package com.github.thomasdarimont.keycloak.custom.auth.checkaccess;

import com.google.auto.service.AutoService;
import lombok.Data;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * Authenticator that can evaluate fixed policies based on client attributes from the current target client.
 *
 * Can be used as the last authenticator within an auth flow section.
 * <p>
 * Supported policies:
 * <ul>
 *     <li>denyIfNotAllowed</li>
 *     <li>allowIfNotDenied</li>
 * </ul>
 * <p>
 * Some examples:
 * <p>For Groups:
 * <p>
 * <pre>
 * accessCheckGroupPolicy: denyIfNotAllowed
 * accessCheckGroupAllowAny: group1,group2
 * </pre>
 * <p>For Roles:
 * <p>
 * <pre>
 * accessCheckRolePolicy: allowIfNotDenied
 * accessCheckRoleDenyAny: role1,role2
 * </pre>
 * <p>For User Attributes:
 * <p>
 * <pre>
 * accessCheckAttributePolicy: denyIfNotAllowed
 * accessCheckAttributeAllowAny: attr1=foo,attr2
 * </pre>
 */
@JBossLog
public class CheckAccessAuthenticator implements Authenticator {

    public static final String ID = "acme-auth-check-access";

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        var session = context.getSession();
        var realm = context.getRealm();
        var user = context.getUser();
        var authSession = context.getAuthenticationSession();
        var client = authSession.getClient();

        for (var check : List.<AccessCheck>of(this::checkAttributes, this::checkRoles, this::checkGroups)) {
            var checkResult = check.apply(session, realm, user, client);

            if (!checkResult.isMatched()) {
                continue;
            }

            log.debugf("Matched check %s allow: %s", checkResult.getName(), checkResult.isAllow());
            if (checkResult.isAllow()) {
                context.success();
            } else {
                context.failure(AuthenticationFlowError.ACCESS_DENIED);
            }
            return;
        }

        // TODO make default allow / deny configurable
        context.success();
    }

    private CheckResult checkGroups(KeycloakSession session, RealmModel realm, UserModel user, ClientModel client) {
        var allowResult = checkGroupsInternal(session, realm, client, user, "accessCheckGroupAllowAny");
        var denyResult = checkGroupsInternal(session, realm, client, user, "accessCheckGroupDenyAny");
        return evaluateCheck("Group", allowResult, denyResult, client);
    }

    private CheckResult checkRoles(KeycloakSession session, RealmModel realm, UserModel user, ClientModel client) {
        var allowResult = checkRolesInternal(session, realm, user, client, "accessCheckRoleAllowAny");
        var denyResult = checkRolesInternal(session, realm, user, client, "accessCheckRoleDenyAny");
        return evaluateCheck("Role", allowResult, denyResult, client);
    }

    private CheckResult checkAttributes(KeycloakSession session, RealmModel realm, UserModel user, ClientModel client) {
        var allowResult = checkAttributeInternal(user, client, "accessCheckAttributeAllowAny");
        var denyResult = checkAttributeInternal(user, client, "accessCheckAttributeDenyAny");
        return evaluateCheck("Attribute", allowResult, denyResult, client);
    }


    private CheckResult evaluateCheck(String check, Boolean allowResult, Boolean denyResult, ClientModel client) {

        var allow = false;
        var matched = true;
        if (allowResult == null && denyResult == null) {
            matched = false;
        } else {
            var policy = client.getAttribute("accessCheck" + check + "Policy");
            if ("denyIfNotAllowed".equals(policy)) {
                allow = allowResult != null && allowResult;
            } else if ("allowIfNotDenied".equals(policy)) {
                allow = denyResult == null || !denyResult;
            }
            log.debugf("Evaluated check: %s with policy: %s. Outcome allow: %s ", check, policy, allow);
        }

        return new CheckResult(check, allow, matched);
    }

    private Boolean checkGroupsInternal(KeycloakSession session, RealmModel realm, ClientModel client, UserModel user, String checkAttributeName) {

        var checkAttribute = client.getAttribute(checkAttributeName);
        if (checkAttribute == null) {
            return null;
        }

        var groupNameEntries = checkAttribute.split(",");

        for (var groupNameEntry : groupNameEntries) {
            var groupName = groupNameEntry.trim();

            // * matches all groups, even empty lists
            if (groupName.equals("*")) {
                return true;
            }
            var group = session.groups().searchForGroupByNameStream(realm, groupName, true, 0, 1).findAny().orElse(null);
            if (group == null) {
                log.debugf("group not found. realm:%s group:%s", realm.getName(), groupName);
                continue;
            }
            if (user.isMemberOf(group)) {
                return true;
            }
        }

        return false;
    }


    private Boolean checkRolesInternal(KeycloakSession session, RealmModel realm, UserModel user, ClientModel client, String checkAttributeName) {

        var checkAttribute = client.getAttribute(checkAttributeName);
        if (checkAttribute == null) {
            return null;
        }

        var roleNameEntries = checkAttribute.split(",");

        for (var roleNameEntry : roleNameEntries) {
            var roleNameCandidate = roleNameEntry.trim();

            // * matches all roles, even empty lists
            if (roleNameCandidate.equals("*")) {
                return true;
            }

            var role = resolveRole(session, realm, client, roleNameCandidate);
            if (role == null) {
                continue;
            }

            if (user.hasRole(role)) {
                return true;
            }
        }

        return false;
    }

    private static RoleModel resolveRole(KeycloakSession session, RealmModel realm, ClientModel client, String roleNameEntry) {

        if (!roleNameEntry.contains(":")) {
            // realm roles can be referred to by "role"
            return session.roles().getRealmRole(realm, roleNameEntry);
        }

        var targetClient = client;
        // detected client role
        var clientWithRole = roleNameEntry.split(":");
        var clientId = clientWithRole[0].trim();
        var roleName = clientWithRole[1].trim();
        if (!clientId.isEmpty()) {
            // other client-roles can be referred with otherClientId:clientRole
            targetClient = session.clients().getClientByClientId(realm, clientId);
        }
        // local client-roles can be referred to by ":clientRole"
        return session.roles().getClientRole(targetClient, roleName);
    }

    private static Boolean checkAttributeInternal(UserModel user, ClientModel client, String checkAttributeName) {

        var checkAttribute = client.getAttribute(checkAttributeName);
        if (checkAttribute == null) {
            return null;
        }

        var attributeValuePairs = checkAttribute.split(",");

        for (var attributeValuePair : attributeValuePairs) {
            var attrValuePair = attributeValuePair.split("=");
            var attributeName = attrValuePair[0].trim();
            var attributeValue = attrValuePair.length > 1 ? attrValuePair[1].trim() : "";
            var value = user.getFirstAttribute(attributeName);
            if (value == null) {
                continue;
            }
            if (attributeValue.equals("*")) {
                // we match every value as long as the attribute exists
                return true;
            }
            value = value.trim();
            if (value.equals(attributeValue)) {
                // exact attribute match
                return true;
            }
        }

        return false;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // NOOP
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }


    @AutoService(AuthenticatorFactory.class)
    public static class Factory extends OTPFormAuthenticatorFactory {

        public static final CheckAccessAuthenticator SINGLETON = new CheckAccessAuthenticator();

        @Override
        public Authenticator create(KeycloakSession session) {
            return SINGLETON;
        }

        @Override
        public String getId() {
            return CheckAccessAuthenticator.ID;
        }

        @Override
        public String getDisplayType() {
            return "Acme: Check Access";
        }

        @Override
        public String getHelpText() {
            return "Checks if the given user has access to the target application.";
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            return null;
        }

        @Override
        public String getReferenceCategory() {
            return "access";
        }

        @Override
        public boolean isUserSetupAllowed() {
            return true;
        }

    }

    interface AccessCheck {
        CheckResult apply(KeycloakSession session, RealmModel realm, UserModel user, ClientModel client);
    }

    @Data
    static class CheckResult {

        private final String name;

        private final boolean allow;

        private final boolean matched;
    }

}
