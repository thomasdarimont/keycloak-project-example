package com.github.thomasdarimont.keycloak.custom.oidc.scopes;

import com.google.auto.service.AutoService;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@AutoService(ProtocolMapper.class)
public class OnlyGrantedScopesMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    private static final String PROVIDER_ID = "oidc-granted-scopes-protocol-mapper";

    private static final Logger LOGGER = Logger.getLogger(OnlyGrantedScopesMapper.class);

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    static {
        var list = new ArrayList<ProviderConfigProperty>();
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(list, OnlyGrantedScopesMapper.class);
        CONFIG_PROPERTIES = list;
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Acme: Ensure only granted scopes";
    }

    @Override
    public String getHelpText() {
        return "A protocol mapper that ensures only granted scopes.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession session, ClientSessionContext clientSessionCtx) {

        var context = session.getContext();
        var user = userSession.getUser();
        var client = clientSessionCtx.getClientSession().getClient();

        var consentByClient = session.users().getConsentByClient(context.getRealm(), user.getId(), client.getId());

        var at = (AccessToken) token;
        var requestedScopeValue = at.getScope();
        var scopeItems = new ArrayList<>(List.of(requestedScopeValue.split(" ")));

        var grantedScopes = consentByClient.getGrantedClientScopes().stream() //
                .map(ClientScopeModel::getName) //
                .collect(Collectors.toList());

        var result = new LinkedHashSet<String>();
        result.add(OAuth2Constants.SCOPE_OPENID);
        for (var requestedScopeItem : scopeItems) {
            if (grantedScopes.contains(requestedScopeItem)) {
                result.add(requestedScopeItem);
            }
        }

        var claimValue = String.join(" ", result);
        at.setScope(claimValue);
    }
}