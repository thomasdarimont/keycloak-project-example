package com.github.thomasdarimont.keycloak.custom.idp.oidc;

import com.google.auto.service.AutoService;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;

import java.io.IOException;

/**
 * PoC for a custom {@link OidcIdentityProvider} that uses an OID claim to link user accounts.
 */
public class AcmeOidcIdentityProvider extends OIDCIdentityProvider {

    public AcmeOidcIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    protected BrokeredIdentityContext extractIdentity(AccessTokenResponse tokenResponse, String accessToken, JsonWebToken idToken) throws IOException {

        String sub = idToken.getId();
        String oid = (String)idToken.getOtherClaims().get("oid");
        idToken.setSubject(oid);

        return super.extractIdentity(tokenResponse, accessToken, idToken);
    }

    // @AutoService(IdentityProviderFactory.class)
    public static class Factory extends OIDCIdentityProviderFactory {

        @Override
        public OIDCIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
            return new AcmeOidcIdentityProvider(session, new OIDCIdentityProviderConfig(model));
        }
    }
}
