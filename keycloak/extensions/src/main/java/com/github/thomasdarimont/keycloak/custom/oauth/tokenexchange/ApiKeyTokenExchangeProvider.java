package com.github.thomasdarimont.keycloak.custom.oauth.tokenexchange;

import com.github.thomasdarimont.keycloak.custom.support.TokenUtils;
import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.DefaultTokenExchangeProvider;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenExchangeProvider;
import org.keycloak.protocol.oidc.TokenExchangeProviderFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;

import java.util.regex.Pattern;

/**
 * PoC for a custom Token-Exchange which can translate an API key of a technical user into an access-token.
 *
 * See custom_token_exchange.http "Perform custom token exchange with API Key"
 */
@JBossLog
@RequiredArgsConstructor
public class ApiKeyTokenExchangeProvider extends DefaultTokenExchangeProvider {

    public static final String ID = "acme-apikey-token-exchange";

    public static final String ALLOWED_CLIENT_ID = "acme-api-gateway";
    public static final String API_KEY_PARAM = "api_key";
    public static final String DEFAULT_SCOPE = "roles";

    private static final Pattern COLON_SPLIT_PATTERN = Pattern.compile(":");

    private final KeycloakSession session;

    @Override
    protected Response exchangeClientToClient(UserModel targetUser, UserSessionModel targetUserSession, AccessToken token, boolean disallowOnHolderOfTokenMismatch) {

        var context = session.getContext();
        var realm = context.getRealm();
        var formParams = context.getHttpRequest().getDecodedFormParameters();

        var apiKey = formParams.getFirst(API_KEY_PARAM);
        if (apiKey == null) {
            return unsupportedResponse();
        }

        var clientId = ALLOWED_CLIENT_ID;
        var scope = DEFAULT_SCOPE;

        var usernameAndKey = COLON_SPLIT_PATTERN.split(apiKey);
        var apiUsername = usernameAndKey[0];
        var user = session.users().getUserByUsername(realm, apiUsername);
        if (user == null) {
            return unsupportedResponse();
        }

        var key = usernameAndKey[1];
        var valid = user.credentialManager().isValid(UserCredentialModel.password(key));
        if (!valid) {
            return unsupportedResponse();
        }

        var accessToken = TokenUtils.generateAccessToken(session, realm, user, clientId, scope, null);

        var tokenResponse = new AccessTokenResponse();
        tokenResponse.setToken(accessToken);
        tokenResponse.setIdToken(null);
        tokenResponse.setRefreshToken(null);
        tokenResponse.setRefreshExpiresIn(0);
        tokenResponse.getOtherClaims().clear();

        return Response.ok(tokenResponse) //
                .type(MediaType.APPLICATION_JSON_TYPE) //
                .build();
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
        return unsupportedResponse();
    }

    private Response unsupportedResponse() {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @Override
    public boolean supports(TokenExchangeContext context) {

        var clientIdMatches = context.getClient() != null && ALLOWED_CLIENT_ID.equals(context.getClient().getClientId());
        if (!clientIdMatches) {
            return false;
        }

        var apiKey = context.getFormParams().getFirst("api_key");
        if (apiKey == null) {
            return false;
        }

        return true;
    }

    @Override
    public void close() {
        // NOOP
    }

    @AutoService(TokenExchangeProviderFactory.class)
    public static class Factory implements TokenExchangeProviderFactory {

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public TokenExchangeProvider create(KeycloakSession session) {
            return new ApiKeyTokenExchangeProvider(session);
        }

        @Override
        public int order() {
            // default order in DefaultTokenExchangeProviderFactory is 0.
            // A higher order ensures we're executed first.
            return 20;
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
