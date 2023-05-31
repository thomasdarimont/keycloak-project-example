package com.github.thomasdarimont.keycloak.custom.oauth.tokenexchange;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.DefaultTokenExchangeProvider;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenExchangeProvider;
import org.keycloak.protocol.oidc.TokenExchangeProviderFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

@JBossLog
public class CustomTokenExchangeProvider extends DefaultTokenExchangeProvider {

    public static final String ID = "acme-token-exchange";

    public static final String ALLOWED_REQUESTED_ISSUER = "https://id.acme.test/offline";

    public static final Set<String> ALLOWED_CLIENT_IDS = Set.of("acme-client-cli-app");

    @Override
    protected Response exchangeClientToClient(UserModel targetUser, UserSessionModel targetUserSession, AccessToken token, boolean disallowOnHolderOfTokenMismatch) {
        return unsupportedResponse();
    }

    @Override
    protected Response exchangeClientToOIDCClient(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType, ClientModel targetClient, String audience, String scope) {
        return unsupportedResponse();
    }

    @Override
    protected Response exchangeClientToSAML2Client(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType, ClientModel targetClient) {
        return unsupportedResponse();
    }

    @Override
    protected Response exchangeExternalToken(String issuer, String subjectToken) {
        return unsupportedResponse();
    }

    @Override
    protected Response exchangeToIdentityProvider(UserModel targetUser, UserSessionModel targetUserSession, String requestedIssuer) {

        // propagate new offline session to mobile_bff
        // obtain new access token & refresh token from mobile_bff
        // TODO ensure keys from mobile_bff JWKS endpoint are combined with keycloak jwks keys

        var accessToken = "appAT";
        var refreshToken = "appRT";

        var tokenResponse = new AccessTokenResponse();
        tokenResponse.setToken(accessToken);
        tokenResponse.setIdToken(null);
        tokenResponse.setRefreshToken(refreshToken);
        tokenResponse.setRefreshExpiresIn(0);
        tokenResponse.getOtherClaims().clear();

        return Response.ok(tokenResponse) //
                .type(MediaType.APPLICATION_JSON_TYPE) //
                .build();
    }

    private Response unsupportedResponse() {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @Override
    public boolean supports(TokenExchangeContext context) {

        var issuerMatches = context.getFormParams() != null && ALLOWED_REQUESTED_ISSUER.equals(context.getFormParams().getFirst(OAuth2Constants.REQUESTED_ISSUER));
        var clientIdMatches = context.getClient() != null && ALLOWED_CLIENT_IDS.contains(context.getClient().getClientId());

        return issuerMatches && clientIdMatches;
    }

    @Override
    public void close() {
        // NOOP
    }

    @AutoService(TokenExchangeProviderFactory.class)
    public static class Factory implements TokenExchangeProviderFactory {

        public static final TokenExchangeProvider INSTANCE = new CustomTokenExchangeProvider();

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public TokenExchangeProvider create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public int order() {
            // default order in DefaultTokenExchangeProviderFactory is 0.
            // A higher order ensures we're executed first.
            return 10;
        }

        @Override
        public void init(Config.Scope config) {
            // NOOP
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            // NOOP
        }

        @Override
        public void close() {
            // NOOP
        }
    }
}
