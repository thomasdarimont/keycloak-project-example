package com.github.thomasdarimont.keycloak.custom.endpoints;

import com.github.thomasdarimont.keycloak.custom.support.KeycloakSessionLookup;
import com.github.thomasdarimont.keycloak.custom.support.ResteasyUtil;
import com.google.auto.service.AutoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import java.util.Optional;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class CustomResourceProvider implements RealmResourceProvider {

    public static final String ID = "custom-resources";

    private static final Pattern ALLOWED_REALM_NAMES_PATTERN = Pattern.compile(Optional.ofNullable(System.getenv("KEYCLOAK_CUSTOM_ENDPOINT_REALM_PATTERN")).orElse("acme-.*"));

    @Override
    public Object getResource() {

        KeycloakSession session = KeycloakSessionLookup.currentSession();

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

        CustomResource customResource = new CustomResource(session, accessToken);
        ResteasyUtil.injectProperties(customResource);
        return customResource;
    }

    AdminPermissionEvaluator getAuth(KeycloakSession session) {
        AdminAuth adminAuth = getAdminAuth(session);
        return AdminPermissions.evaluator(session, session.getContext().getRealm(), adminAuth);
    }

    private static AdminAuth getAdminAuth(KeycloakSession session) {
        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
        return new AdminAuth(session.getContext().getRealm(), authResult.getToken(), authResult.getUser(), authResult.getClient());
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
