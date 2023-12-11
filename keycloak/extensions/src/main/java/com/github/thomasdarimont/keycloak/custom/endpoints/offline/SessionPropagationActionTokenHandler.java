package com.github.thomasdarimont.keycloak.custom.endpoints.offline;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.ActionTokenHandlerFactory;
import org.keycloak.authentication.actiontoken.DefaultActionToken;
import org.keycloak.authentication.actiontoken.TokenUtils;
import org.keycloak.common.util.Time;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;

import jakarta.ws.rs.core.Response;
import java.net.URI;

@JBossLog
@AutoService(ActionTokenHandlerFactory.class)
public class SessionPropagationActionTokenHandler extends AbstractActionTokenHandler<SessionPropagationActionToken> {

    private static final String ERROR_SESSION_PROPAGATION = "errorSessionPropagation";

    public SessionPropagationActionTokenHandler() {
        super(SessionPropagationActionToken.TOKEN_TYPE, SessionPropagationActionToken.class, ERROR_SESSION_PROPAGATION, EventType.CLIENT_LOGIN, Errors.NOT_ALLOWED);
    }

    @Override
    public Response handleToken(SessionPropagationActionToken token, ActionTokenContext<SessionPropagationActionToken> tokenContext) {

        var session = tokenContext.getSession();
        var realm = tokenContext.getRealm();
        var clientConnection = tokenContext.getClientConnection();

        // mark token as consumed
        var singleUseObjectProvider = session.getProvider(SingleUseObjectProvider.class);
        singleUseObjectProvider.put(token.serializeKey(), token.getExp() - Time.currentTime() + 1, null); // mark token as invalidated, +1 second to account for rounding to seconds

        var authSession = tokenContext.getAuthenticationSession();
        var authenticatedUser = authSession.getAuthenticatedUser();
        var redirectUri = token.getRedirectUri();

        // check for existing user session
        var authResult = AuthenticationManager.authenticateIdentityCookie(session, realm, true);
        if (authResult != null) {

            if (!authenticatedUser.getId().equals(authResult.getUser().getId())) {
                // detected existing user session for different user, abort propagation.
                log.warnf("Skipped Offline-Session to User-Session propagation detected existing session for different user. realm=%s userId=%s sourceClientId=%s targetClientId=%s", authSession.getRealm().getName(), authenticatedUser.getId(), token.getSourceClientId(), token.getIssuedFor());
                throw new ErrorPageException(session, authSession, Response.Status.BAD_REQUEST, Messages.DIFFERENT_USER_AUTHENTICATED, authResult.getUser().getUsername());
            }

            // detected existing session for current user, reuse the existing session instead of creating a new one.
            log.infof("Skipped Offline-Session to User-Session propagation due to existing session. realm=%s userId=%s sourceClientId=%s targetClientId=%s", authSession.getRealm().getName(), authenticatedUser.getId(), token.getSourceClientId(), token.getIssuedFor());
            return redirectTo(redirectUri);
        }

        // no user session found so create a new one.
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, token.getIssuer());
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, "openid");

        var rememberMe = token.getRememberMe();
        var userSession = session.sessions().createUserSession(null, realm, authSession.getAuthenticatedUser(), authSession.getAuthenticatedUser().getUsername(), clientConnection.getRemoteAddr(), OIDCLoginProtocol.LOGIN_PROTOCOL, rememberMe, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);

        AuthenticationManager.setClientScopesInSession(authSession);
        AuthenticationManager.createLoginCookie(session, realm, userSession.getUser(), userSession, tokenContext.getUriInfo(), clientConnection);

        log.infof("Propagated Offline-Session to User-Session. realm=%s userId=%s sourceClientId=%s targetClientId=%s", authSession.getRealm().getName(), authenticatedUser.getId(), token.getSourceClientId(), token.getIssuedFor());

        return redirectTo(redirectUri);
    }

    private Response redirectTo(String redirectUri) {
        return Response.temporaryRedirect(URI.create(redirectUri)).build();
    }

    @Override
    public TokenVerifier.Predicate<? super SessionPropagationActionToken>[] getVerifiers(ActionTokenContext<SessionPropagationActionToken> tokenContext) {
        // TODO add additional checks if necessary
        return TokenUtils.predicates(DefaultActionToken.ACTION_TOKEN_BASIC_CHECKS);
    }

    @Override
    public boolean canUseTokenRepeatedly(SessionPropagationActionToken token, ActionTokenContext<SessionPropagationActionToken> tokenContext) {
        return false;
    }

}
