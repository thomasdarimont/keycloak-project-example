package com.github.thomasdarimont.keycloak.custom.oauth.tokenexchange;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenExchangeProvider;
import org.keycloak.protocol.oidc.TokenExchangeProviderFactory;
import org.keycloak.protocol.oidc.tokenexchange.StandardTokenExchangeProvider;
import org.keycloak.protocol.oidc.tokenexchange.StandardTokenExchangeProviderFactory;
import org.keycloak.representations.AccessToken;

import java.util.List;

@JBossLog
public class CustomV2TokenExchangeProvider extends StandardTokenExchangeProvider {

    @Override
    protected Response exchangeClientToOIDCClient(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType, List<ClientModel> targetAudienceClients, String scope, AccessToken subjectToken) {
        return super.exchangeClientToOIDCClient(targetUser, targetUserSession, requestedTokenType, targetAudienceClients, scope, subjectToken);
    }

    // @AutoService(TokenExchangeProviderFactory.class)
    public static class Factory extends StandardTokenExchangeProviderFactory {

        @Override
        public TokenExchangeProvider create(KeycloakSession session) {
            return new CustomV2TokenExchangeProvider();
        }

        static {
            log.debug("Initializing CustomV2TokenExchangeProvider");
        }
    }
}
