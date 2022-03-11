package com.github.thomasdarimont.keycloak.custom.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.authentication.actiontoken.DefaultActionToken;

public class RequestAccountDeletionActionToken extends DefaultActionToken {

    public static final String TOKEN_TYPE = "acme-request-accountdeletion";

    private static final String REDIRECT_URI = "acme:redirect-uri";

    public RequestAccountDeletionActionToken(String userId, int absoluteExpirationInSecs, String clientId, String redirectUri) {
        super(userId, TOKEN_TYPE, absoluteExpirationInSecs, null);
        this.issuedFor = clientId;
        setRedirectUri(redirectUri);
    }

    /**
     * Required for deserialization.
     */
    @SuppressWarnings("unused")
    private RequestAccountDeletionActionToken() {
    }

    @JsonProperty(REDIRECT_URI)
    public String getRedirectUri() {
        return (String) getOtherClaims().get(REDIRECT_URI);
    }

    @JsonProperty(REDIRECT_URI)
    public final void setRedirectUri(String redirectUri) {
        if (redirectUri != null) {
            setOtherClaims(REDIRECT_URI, redirectUri);
            return;
        }
        getOtherClaims().remove(REDIRECT_URI);
    }

}
