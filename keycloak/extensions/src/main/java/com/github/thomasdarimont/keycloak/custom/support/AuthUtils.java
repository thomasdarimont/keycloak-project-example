package com.github.thomasdarimont.keycloak.custom.support;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

public class AuthUtils {

    public static AdminPermissionEvaluator getAdminPermissionEvaluator(KeycloakSession session) {
        return AdminPermissions.evaluator(session, session.getContext().getRealm(), getAdminAuth(session));
    }

    public static AdminAuth getAdminAuth(KeycloakSession session) {
        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
        return new AdminAuth(session.getContext().getRealm(), authResult.getToken(), authResult.getUser(), authResult.getClient());
    }
}
