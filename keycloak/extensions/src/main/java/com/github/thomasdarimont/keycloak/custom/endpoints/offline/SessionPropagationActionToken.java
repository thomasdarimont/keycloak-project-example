package com.github.thomasdarimont.keycloak.custom.endpoints.offline;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.authentication.actiontoken.DefaultActionToken;

public class SessionPropagationActionToken extends DefaultActionToken {

    private static final String CLAIM_PREFIX = "acme:";

    public static final String TOKEN_TYPE = "acme-session-propagation";

    private static final String REDIRECT_URI = CLAIM_PREFIX + "redirect-uri";

    private static final String SOURCE_CLIENT_ID = CLAIM_PREFIX + "sourceClientId";

    private static final String REMEMBER_ME = CLAIM_PREFIX + "rememberMe";

    public SessionPropagationActionToken(String userId, int absoluteExpirationInSecs, String clientId, String redirectUri, String sourceClientId, Boolean rememberMe) {
        super(userId, TOKEN_TYPE, absoluteExpirationInSecs, null);
        this.issuedFor = clientId;
        setRedirectUri(redirectUri);
        setSourceClientId(sourceClientId);
        setRememberMe(rememberMe);
    }

    /**
     * Required for deserialization.
     */
    @SuppressWarnings("unused")
    private SessionPropagationActionToken() {
    }

    @JsonProperty(REDIRECT_URI)
    public String getRedirectUri() {
        return (String) getOtherClaims().get(REDIRECT_URI);
    }

    @JsonProperty(REDIRECT_URI)
    public void setRedirectUri(String redirectUri) {
        if (redirectUri != null) {
            setOtherClaims(REDIRECT_URI, redirectUri);
            return;
        }
        getOtherClaims().remove(REDIRECT_URI);
    }

    @JsonProperty(SOURCE_CLIENT_ID)
    public String getSourceClientId() {
        return (String) getOtherClaims().get(SOURCE_CLIENT_ID);
    }

    @JsonProperty(SOURCE_CLIENT_ID)
    public void setSourceClientId(String clientId) {
        getOtherClaims().put(SOURCE_CLIENT_ID, clientId);
    }

    @JsonProperty(REMEMBER_ME)
    public boolean getRememberMe() {
        return Boolean.parseBoolean(String.valueOf(getOtherClaims().get(REMEMBER_ME)));
    }

    @JsonProperty(REMEMBER_ME)
    public void setRememberMe(Boolean rememberMe) {
        getOtherClaims().put(REMEMBER_ME, rememberMe);
    }

}
