package com.github.thomasdarimont.keycloak.custom.oidc.authzenclaims;

import com.github.thomasdarimont.keycloak.custom.auth.authzen.AuthZen;
import com.github.thomasdarimont.keycloak.custom.auth.authzen.AuthzenClient;
import com.github.thomasdarimont.keycloak.custom.auth.opa.OpaClient;
import com.github.thomasdarimont.keycloak.custom.config.MapConfig;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.IDToken;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@JBossLog
@AutoService(ProtocolMapper.class)
public class AuthzenClaimMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    static {
        var list = ProviderConfigurationBuilder.create() //

                .property().name(AuthzenClient.AUTHZ_URL) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("Authzen Policy URL") //
                .defaultValue(OpaClient.DEFAULT_OPA_AUTHZ_URL) //
                .helpText("URL of Authzen Policy URL") //
                .add() //

                .property().name(AuthzenClient.AUTHZ_TYPE) //
                .type(ProviderConfigProperty.LIST_TYPE) //
                .label("Authzen call type") //
                .options(AuthzenClient.AUTHZ_TYPE_ACCESS, AuthzenClient.AUTHZ_TYPE_SEARCH)
                .defaultValue(AuthzenClient.AUTHZ_TYPE_ACCESS) //
                .helpText("Type of the Authzen API call") //
                .add() //

                .property().name(AuthzenClient.USER_ATTRIBUTES) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("User Attributes") //
                .defaultValue(null) //
                .helpText("Comma separated list of user attributes to send with authz requests.") //
                .add() //

                .property().name(AuthzenClient.ACTION) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("Action") //
                .defaultValue(null) //
                .helpText("Name fo the action to check.") //
                .add() //

                .property().name(AuthzenClient.DESCRIPTION) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("Description") //
                .defaultValue(null) //
                .helpText("Description.") //
                .add() //

                .property().name(AuthzenClient.RESOURCE_TYPE) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("Resource Type") //
                .defaultValue(null) //
                .helpText("The resource type to access.") //
                .add() //

                .property().name(AuthzenClient.RESOURCE_CLAIM_NAME) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("Resource Claim Name") //
                .defaultValue(null) //
                .helpText("Name of the claim to extract the resource claims.") //
                .add() //

                .property().name(AuthzenClient.REALM_ATTRIBUTES) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("Realm Attributes") //
                .defaultValue(null) //
                .helpText("Comma separated list of realm attributes to send with authz requests.") //
                .add() //

                .property().name(AuthzenClient.CONTEXT_ATTRIBUTES) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("Context Attributes") //
                .defaultValue(null) //
                .helpText("Comma separated list of context attributes to send with authz requests. Supported attributes: remoteAddress") //
                .add() //

                .property().name(AuthzenClient.REQUEST_HEADERS) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("Request Headers") //
                .defaultValue(null) //
                .helpText("Comma separated list of request headers to send with authz requests.") //
                .add() //

                .property().name(AuthzenClient.CLIENT_ATTRIBUTES) //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .label("Client Attributes") //
                .defaultValue(null) //
                .helpText("Comma separated list of client attributes to send with authz requests.") //
                .add() //

                .property().name(AuthzenClient.USE_REALM_ROLES) //
                .type(ProviderConfigProperty.BOOLEAN_TYPE) //
                .label("Use realm roles") //
                .defaultValue("true") //
                .helpText("If enabled, realm roles will be sent with authz requests.") //
                .add() //

                .property().name(AuthzenClient.USE_CLIENT_ROLES) //
                .type(ProviderConfigProperty.BOOLEAN_TYPE) //
                .label("Use client roles") //
                .defaultValue("true") //
                .helpText("If enabled, client roles will be sent with authz requests.") //
                .add() //

                .property().name(AuthzenClient.USE_GROUPS) //
                .type(ProviderConfigProperty.BOOLEAN_TYPE) //
                .label("Use groups") //
                .defaultValue("true") //
                .helpText("If enabled, group information will be sent with authz requests.") //
                .add() //

                .property().name(AuthzenClient.USE_USER_ATTRIBUTES) //
                .type(ProviderConfigProperty.BOOLEAN_TYPE) //
                .label("Use user attributes") //
                .defaultValue("true") //
                .helpText("If enabled, user attribute information will be sent with authz requests based on the selection of user attributes.") //
                .add() //

                .build();

        OIDCAttributeMapperHelper.addAttributeConfig(list, UserPropertyMapper.class);

        CONFIG_PROPERTIES = Collections.unmodifiableList(list);
    }

    @Override
    public String getId() {
        return "acme-oidc-authzen-mapper";
    }

    @Override
    public String getDisplayType() {
        return "Acme: Authzen Claim Mapper";
    }

    @Override
    public String getHelpText() {
        return "Uses the Authzen Search API to obtain claims to add to the Token";
    }

    @Override
    public String getDisplayCategory() {
        return "authzen";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        var context = keycloakSession.getContext();
        var realm = context.getRealm();
        var authSession = clientSessionCtx.getClientSession();
        var user = authSession.getUserSession().getUser();
        var config = new MapConfig(mappingModel.getConfig());
        String action = mappingModel.getConfig().get(AuthzenClient.ACTION);
        String resourceType = mappingModel.getConfig().get(AuthzenClient.RESOURCE_TYPE);
        var resource = new AuthZen.Resource(resourceType);
        var client = authSession.getClient();
        var authZenClient = new AuthzenClient();

        String authzenType = mappingModel.getConfig().get(AuthzenClient.AUTHZ_TYPE);
        switch (authzenType) {
            case AuthzenClient.AUTHZ_TYPE_ACCESS -> {
                var accessResponse = authZenClient.checkAccess(keycloakSession, config, realm, user, client, action, resource);
                if (accessResponse == null) {
                    return;
                }
                copyAccessResultToClaim(token, config, accessResponse);
            }
            case AuthzenClient.AUTHZ_TYPE_SEARCH -> {
                var searchResponse = authZenClient.search(keycloakSession, config, realm, user, client, action, resource);
                if (searchResponse == null || CollectionUtil.isEmpty(searchResponse.results())) {
                    return;
                }
                copySearchResultToClaim(token, config, searchResponse);
            }
            case null -> {
                // NOOP
            }
            default -> throw new IllegalStateException("Unexpected value: " + authzenType);
        }

    }

    protected void copyAccessResultToClaim(IDToken token, MapConfig config, AuthZen.AccessResponse accessResponse) {
        // TODO implement me
    }

    protected void copySearchResultToClaim(IDToken token, MapConfig config, AuthZen.SearchResponse searchResponse) {
        String targetClaimName = config.getString("claim.name");
        String sourceClaimName = config.getString(AuthzenClient.RESOURCE_CLAIM_NAME);

        List<AuthZen.Resource> results = searchResponse.results();
        Set<Object> values = new LinkedHashSet<>(results.size());
        for (AuthZen.Resource result : results) {

            if ("id".equals(sourceClaimName)) {
                values.add(result.id());
            } else if ("type".equals(sourceClaimName)) {
                values.add(result.type());
            } else {
                if (result.properties() != null) {
                    Object value = result.properties().get(sourceClaimName);
                    values.add(value);
                }
            }
        }

        token.setOtherClaims(targetClaimName, values);
    }
}
