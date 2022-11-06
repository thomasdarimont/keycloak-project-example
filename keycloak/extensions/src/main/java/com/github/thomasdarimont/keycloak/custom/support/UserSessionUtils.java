package com.github.thomasdarimont.keycloak.custom.support;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.AuthenticationSessionModel;

public class UserSessionUtils {

    public static UserSessionModel getUserSessionFromAuthenticationSession(KeycloakSession session, AuthenticationSessionModel authSession) {
        return session.sessions().getUserSession(authSession.getRealm(), authSession.getParentSession().getId());
    }
}
