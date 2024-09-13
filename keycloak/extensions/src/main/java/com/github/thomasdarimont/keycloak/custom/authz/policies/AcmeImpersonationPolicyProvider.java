package com.github.thomasdarimont.keycloak.custom.authz.policies;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.attribute.Attributes;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.evaluation.DefaultEvaluation;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.utils.KeycloakSessionUtil;

import java.util.List;

@JBossLog
public class AcmeImpersonationPolicyProvider implements PolicyProvider {

    private final KeycloakSession session;
    private final AuthorizationProvider authorization;

    public AcmeImpersonationPolicyProvider(KeycloakSession session, AuthorizationProvider authorization) {
        this.session = session;
        this.authorization = authorization;
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        log.info("Evaluate");

        List<String> requestedScopeNames = ((DefaultEvaluation) evaluation).getParentPolicy().getScopes().stream().map(Scope::getName).toList();

        boolean userImpersonation = requestedScopeNames.size() == 1 && requestedScopeNames.contains("user-impersonated"); // UserPermissions.USER_IMPERSONATED_SCOPE is currently not public...
        boolean adminImpersonation = !userImpersonation;

        Attributes attributes = evaluation.getContext().getIdentity().getAttributes();
        String fromUserId = attributes.getValue("sub").asString(0);
        String fromUsername = attributes.getValue("preferred_username").asString(0);

        KeycloakContext keycloakContext = session.getContext();
        MultivaluedMap<String, String> formParameters = keycloakContext.getHttpRequest().getDecodedFormParameters();
        String toUserId = formParameters.getFirst(OAuth2Constants.REQUESTED_SUBJECT);
        String requestedTokenType = formParameters.getFirst(OAuth2Constants.REQUESTED_TOKEN_TYPE);

        RealmModel realm = keycloakContext.getRealm();
        UserModel sourceUser = session.users().getUserById(realm, fromUserId);
        UserModel targetUser = session.users().getUserById(realm, toUserId);
        log.debugf("Check user impersonation. realm=%s impersonator=%s targetUsername=%s", realm.getName(), sourceUser.getUsername(), targetUser.getUsername());
        if (isImpersonationAllowed(realm, sourceUser, targetUser)) {
            log.debugf("User impersonation granted. realm=%s impersonator=%s targetUsername=%s", realm.getName(), sourceUser.getUsername(), targetUser.getUsername());
            evaluation.grant();
        } else {
            log.debugf("User impersonation denied. realm=%s impersonator=%s targetUsername=%s", realm.getName(), sourceUser.getUsername(), targetUser.getUsername());
            evaluation.deny();
        }
    }

    protected boolean isImpersonationAllowed(RealmModel realm, UserModel sourceUser, UserModel targetUser) {



        // TODO implement your custom impersonation logic here

        return true;
    }

    @Override
    public void close() {
        // NOOP
    }

    public static class AcmeImpersonationPolicyRepresentation extends JSPolicyRepresentation {
// JSPolicyRepresentation to inherit the code option
    }

    @AutoService(PolicyProviderFactory.class)
    public static class Factory implements PolicyProviderFactory<AcmeImpersonationPolicyRepresentation> {

        @Override
        public String getId() {
            return "acme-impersonation-policy";
        }

        @Override
        public String getName() {
            return "Acme: Impersonation";
        }

        @Override
        public String getGroup() {
            return "Custom";
        }

        @Override
        public PolicyProvider create(KeycloakSession session) {
            return create(session, null);
        }

        @Override
        public PolicyProvider create(AuthorizationProvider authorization) {
            return create(KeycloakSessionUtil.getKeycloakSession(), authorization);
        }

        public PolicyProvider create(KeycloakSession session, AuthorizationProvider authorization) {
            return new AcmeImpersonationPolicyProvider(session, authorization);
        }

        @Override
        public void init(Config.Scope config) {

        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {

        }

        @Override
        public AcmeImpersonationPolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
            var rep = new AcmeImpersonationPolicyRepresentation();
            rep.setId(policy.getId());
            rep.setName(policy.getName());
            rep.setDescription(policy.getDescription());
            rep.setDecisionStrategy(policy.getDecisionStrategy());
            rep.setCode(policy.getConfig().get("code"));
            rep.setType(policy.getType());
            return rep;
        }

        @Override
        public void onUpdate(Policy policy, AcmeImpersonationPolicyRepresentation representation, AuthorizationProvider authorization) {
            policy.setDecisionStrategy(representation.getDecisionStrategy());
            policy.setDescription(policy.getDescription());
            policy.setLogic(policy.getLogic());
        }

        @Override
        public Class<AcmeImpersonationPolicyRepresentation> getRepresentationType() {
            return AcmeImpersonationPolicyRepresentation.class;
        }

        @Override
        public void close() {
            // NOOP
        }
    }
}
