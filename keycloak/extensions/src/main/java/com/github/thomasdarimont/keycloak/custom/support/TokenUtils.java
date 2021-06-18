package com.github.thomasdarimont.keycloak.custom.support;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.util.List;
import java.util.function.Consumer;

public class TokenUtils {

    public static String generateAccessToken(KeycloakSession session, UserSessionModel userSession, String clientId, Consumer<AccessToken> tokenAdjuster) {

        RealmModel realm = userSession.getRealm();
        ClientModel client = session.clients().getClientByClientId(realm, clientId);
        String issuer = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());

        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel iamAuthSession = rootAuthSession.createAuthenticationSession(client);

        iamAuthSession.setAuthenticatedUser(userSession.getUser());
        iamAuthSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        iamAuthSession.setClientNote(OIDCLoginProtocol.ISSUER, issuer);
        iamAuthSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, "iam");

        UserSessionModel iamUserSession = session.sessions().createUserSession(
                iamAuthSession.getParentSession().getId(), realm, userSession.getUser(), userSession.getUser().getUsername(),
                "127.0.0.1", ServiceAccountConstants.CLIENT_AUTH, false, null, null,
                UserSessionModel.SessionPersistenceState.TRANSIENT);

        AuthenticationManager.setClientScopesInSession(iamAuthSession);
        ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(session, iamUserSession, iamAuthSession);

        // Notes about client details
        userSession.setNote(ServiceAccountConstants.CLIENT_ID, client.getClientId());
        userSession.setNote(ServiceAccountConstants.CLIENT_HOST, "127.0.0.1");
        userSession.setNote(ServiceAccountConstants.CLIENT_ADDRESS, "127.0.0.1");

        TokenManager tokenManager = new TokenManager();

        EventBuilder eventBuilder = new EventBuilder(realm, session, InternalClientConnection.INSTANCE);
        TokenManager.AccessTokenResponseBuilder tokenResponseBuilder = tokenManager.responseBuilder(realm, client, eventBuilder, session, iamUserSession, clientSessionCtx);
        AccessToken accessToken = tokenResponseBuilder.generateAccessToken().getAccessToken();

        tokenAdjuster.accept(accessToken);

        AccessTokenResponse tokenResponse = tokenResponseBuilder.build();

        return tokenResponse.getToken();
    }

    static class InternalClientConnection implements ClientConnection {

        static ClientConnection INSTANCE = new InternalClientConnection();

        @Override
        public String getRemoteAddr() {
            return "127.0.0.1";
        }

        @Override
        public String getRemoteHost() {
            return "127.0.0.1";
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public String getLocalAddr() {
            return null;
        }

        @Override
        public int getLocalPort() {
            return 0;
        }
    }
}
