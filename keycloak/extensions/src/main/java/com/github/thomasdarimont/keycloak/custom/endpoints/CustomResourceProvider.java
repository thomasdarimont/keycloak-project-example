package com.github.thomasdarimont.keycloak.custom.endpoints;

import com.github.thomasdarimont.keycloak.custom.support.AuthUtils;
import com.google.auto.service.AutoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.utils.KeycloakSessionUtil;

import java.util.Optional;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class CustomResourceProvider implements RealmResourceProvider {

    public static final String ID = "custom-resources";

    private static final Pattern ALLOWED_REALM_NAMES_PATTERN = Pattern.compile(Optional.ofNullable(System.getenv("KEYCLOAK_CUSTOM_ENDPOINT_REALM_PATTERN")).orElse("acme-.*"));

    @Override
    public Object getResource() {

        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();

        AccessToken accessToken = Tokens.getAccessToken(session);

        // check access
//        if (accessToken == null) {
//            throw new NotAuthorizedException("Invalid Token", Response.status(UNAUTHORIZED).build());
//        } else if (!ScopeUtils.hasScope("custom.api", accessToken.getScope())) {
//            throw new ForbiddenException("No Access", Response.status(FORBIDDEN).build());
//        }

        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            return null;
        }
        boolean allowedRealm = ALLOWED_REALM_NAMES_PATTERN.matcher(realm.getName()).matches();
        if (!allowedRealm) {
            // only expose custom endpoints for allowed realms
            return null;
        }

        return new CustomResource(session, accessToken);
    }

    AdminPermissionEvaluator getAuth(KeycloakSession session) {
        AdminAuth adminAuth = AuthUtils.getAdminAuth(session);
        return AdminPermissions.evaluator(session, session.getContext().getRealm(), adminAuth);
    }

    @Override
    public void close() {
        // NOOP
    }

    @JBossLog
    @AutoService(RealmResourceProviderFactory.class)
    public static class Factory implements RealmResourceProviderFactory {

        private static final CustomResourceProvider INSTANCE = new CustomResourceProvider();

        @Override
        public String getId() {
            return CustomResourceProvider.ID;
        }

        @Override
        public RealmResourceProvider create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public void init(Config.Scope config) {
            // NOOP

        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            log.info("Initialize");
        }

        @Override
        public void close() {
            // NOOP
        }
    }

}
