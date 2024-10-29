package com.github.thomasdarimont.keycloak.custom.oauth.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.jbosslog.JBossLog;
import org.apache.http.HttpStatus;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.representations.AccessTokenResponse;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@JBossLog
@Getter
@Setter
@RequiredArgsConstructor
public class OauthClientCredentialsTokenManager {

    private static final int EXPIRATION_SLACK_SECONDS = 0;

    private String clientId;

    private String tokenUrl;

    private String scope;

    private boolean useCache;

    private String clientSecret;

    private String clientAssertion;

    private String clientAssertionType;

    private String customHttpClientProviderId;

    public String getToken(KeycloakSession session) {

        SingleUseObjectProvider cache = null;
        String tokenKey = createTokenCacheKey(session);

        if (useCache) {
            cache = session.getProvider(SingleUseObjectProvider.class);
            Map<String, String> cachedAccessToken = cache.get(tokenKey);
            if (cachedAccessToken != null) {
                log.debugf("Fetched tokens from cache. tokenKey=%s", tokenKey);
                String accessToken = cachedAccessToken.get(OAuth2Constants.ACCESS_TOKEN);
                return accessToken;
            }
            log.debugf("Could not fetch tokens from cache. tokenKey=%s", tokenKey);
        }

        AccessTokenResponse accessTokenResponse = fetchToken(session, tokenKey);
        String accessToken = accessTokenResponse.getToken();

        if (useCache) {
            // store token
            long expiresInSeconds = accessTokenResponse.getExpiresIn();

            // let's timeout the cached token a bit earlier than it actually does to avoid stale tokens
            long lifespanSeconds = Math.max(expiresInSeconds - EXPIRATION_SLACK_SECONDS, 0);

            Map<String, String> tokenData = Map.of( //
                    OAuth2Constants.ACCESS_TOKEN, accessToken, //
                    OAuth2Constants.EXPIRES_IN, Duration.ofSeconds(expiresInSeconds).toString(), //
                    OAuth2Constants.SCOPE, accessTokenResponse.getScope(), "fetchedAtInstant", Instant.now().toString() //
            );

            cache.put(tokenKey, lifespanSeconds, tokenData);
            log.debugf("Stored new tokens in cache. tokenKey=%s cacheLifespanSeconds=%s", tokenKey, lifespanSeconds);
        }

        return accessToken;
    }

    private String createTokenCacheKey(KeycloakSession session) {
        String realmName = session.getContext().getRealm().getName();
        String cacheKey = "tokens:" + realmName + ":" + clientId + ":" + Integer.toString(tokenUrl.hashCode(), 32);
        return cacheKey;
    }

    protected AccessTokenResponse fetchToken(KeycloakSession session, String tokenKey) {

        KeycloakSession keycloakSession = session;
        if (customHttpClientProviderId != null) {
            // create proxy to intercept calls to keycloakSession.getProvider(HttpClientProvider.class)
            // this allows to easily serve custom http client providers that can use custom client certificates for MTLS auth etc.
            keycloakSession = createKeycloakSessionProxy(session);
        }

        SimpleHttp request = SimpleHttp.doPost(tokenUrl, keycloakSession);
        request.param(OAuth2Constants.CLIENT_ID, clientId);
        request.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS);

        if (clientSecret != null) {
            request.param(OAuth2Constants.CLIENT_SECRET, clientSecret);
        }

        if (clientAssertion != null) {
            request.param(OAuth2Constants.CLIENT_ASSERTION, clientAssertion);
        }

        if (clientAssertionType != null) {
            request.param(OAuth2Constants.CLIENT_ASSERTION_TYPE, clientAssertionType);
        }

        request.param(OAuth2Constants.SCOPE, scope);

        // TODO wrap this around a retry with exponatial backoff in case of HTTP Status 429 / 503 / etc.
        {
            AccessTokenResponse accessTokenResponse = null;
            try {
                SimpleHttp.Response response = request.asResponse();
                if (response.getStatus() != HttpStatus.SC_OK) {
                    throw new RuntimeException("Token retrieval failed: Bad status. status=" + response.getStatus() + " tokenKey=" + tokenKey);
                }
                accessTokenResponse = response.asJson(AccessTokenResponse.class);
                log.debugf("Fetched new tokens. tokenKey=%s", tokenKey);
            } catch (IOException e) {
                throw new RuntimeException("Token retrieval failed: I/O Error. tokenKey=" + tokenKey, e);
            }

            return accessTokenResponse;
        }
    }

    private KeycloakSession createKeycloakSessionProxy(KeycloakSession target) {

        ClassLoader cl = getClass().getClassLoader();
        Class[] ifaces = {KeycloakSession.class};
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {

            if ("getProvider".equals(method.getName()) && args.length == 1 && HttpClientProvider.class.equals(args[0])) {
                HttpClientProvider customHttpClientProvider = target.getProvider(HttpClientProvider.class, customHttpClientProviderId);
                return customHttpClientProvider;
            }

            return method.invoke(target, args);
        };
        Object sessionProxy = Proxy.newProxyInstance(cl, ifaces, handler);
        return KeycloakSession.class.cast(sessionProxy);
    }
}
