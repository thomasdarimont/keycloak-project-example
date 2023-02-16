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
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.core.Response;
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
        singleUseObjectProvider.put(token.serializeKey(), token.getExp() - Time.currentTime(), null); // Token is invalidated

        var authSession = tokenContext.getAuthenticationSession();
        var authenticatedUser = authSession.getAuthenticatedUser();

        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, token.getIssuer());
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, "openid");

        var userSession = session.sessions().createUserSession(realm, authSession.getAuthenticatedUser(), authSession.getAuthenticatedUser().getUsername(), clientConnection.getRemoteAddr(), OIDCLoginProtocol.LOGIN_PROTOCOL, false, null, null);

        AuthenticationManager.setClientScopesInSession(authSession);
        AuthenticationManager.createLoginCookie(session, realm, userSession.getUser(), userSession, tokenContext.getUriInfo(), clientConnection);

        log.infof("Propagated Offline-Session to User-Session. realm=%s userId=%s sourceClientId=%s targetClientId=%s", authSession.getRealm().getName(), authenticatedUser.getId(), token.getSourceClientId(), token.getIssuedFor());

        return Response.temporaryRedirect(URI.create(token.getRedirectUri())).build();
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
