package com.github.thomasdarimont.keycloak.custom.ubersession;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.TokenVerifier;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.UserSessionManager;

@JBossLog
public class UberSessionTokenVerifier {


    public static UberSessionToken verifyUberSessionToken(KeycloakSession session, RealmModel realm, UriInfo uriInfo, ClientConnection connection, boolean checkActive, boolean checkTokenType,
                                                          String checkAudience, boolean isCookie, String tokenString, HttpHeaders headers, TokenVerifier.Predicate<? super JsonWebToken>... additionalChecks) {
        try {
            TokenVerifier<UberSessionToken> verifier = TokenVerifier.create(tokenString, UberSessionToken.class)
                    .withDefaultChecks()
                    .realmUrl(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()))
                    .checkActive(checkActive)
                    .checkTokenType(checkTokenType)
                    .withChecks(additionalChecks);

            if (checkAudience != null) {
                verifier.audience(checkAudience);
            }

            // Check token revocation in case of access token
            if (checkTokenType) {
                verifier.withChecks(new TokenManager.TokenRevocationCheck(session));
            }

            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();

            SignatureVerifierContext signatureVerifier = session.getProvider(SignatureProvider.class, algorithm).verifier(kid);
            verifier.verifierContext(signatureVerifier);

            UberSessionToken token = verifier.verify().getToken();
            if (checkActive) {
                if (!token.isActive() || token.getIssuedAt() < realm.getNotBefore()) {
                    log.debugf("Uber cookie expired. Token expiration: %d, Current Time: %d. token issued at: %d, realm not before: %d",
                            token.getExp(), Time.currentTime(), token.getIssuedAt(), realm.getNotBefore());
                    return null;
                }
            }

            UserModel user = session.users().getUserById(realm, token.getSubject());
            if (user == null || !user.isEnabled()) {
                return null;
            }

            UserSessionModel userSession = session.sessions().createUserSession(null, realm, user, user.getUsername(), connection.getRemoteAddr(), "ubersession", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);

            AuthenticationManager.createLoginCookie(session, realm, user, userSession, session.getContext().getUri(), connection);

            return token;
        } catch (VerificationException e) {
            log.debugf("Failed to verify identity token: %s", e.getMessage());
        }
        return null;
    }

}
