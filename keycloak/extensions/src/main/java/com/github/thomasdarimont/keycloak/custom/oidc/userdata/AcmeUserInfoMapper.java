package com.github.thomasdarimont.keycloak.custom.oidc.userdata;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.IDToken;

import java.util.List;

@JBossLog
@AutoService(ProtocolMapper.class)
public class AcmeUserInfoMapper extends AbstractOIDCProtocolMapper implements UserInfoTokenMapper {

    private static final String PROVIDER_ID = "oidc-acme-userdata-mapper";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    static {
        CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
                .build();

        OIDCAttributeMapperHelper.addAttributeConfig(CONFIG_PROPERTIES, UserPropertyMapper.class);
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Acme Userdata Mapper";
    }

    @Override
    public String getHelpText() {
        return "A protocol mapper that adds additional claims to userinfo";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession session, ClientSessionContext clientSessionCtx) {

        // extract information from httpRequest

        KeycloakContext context = session.getContext();

        // Resteasy.getContextData(HttpRequest.class).getFormParameters().getFirst("acme_supplier_id");

        boolean userInfoEndpointRequest = context.getUri().getPath().endsWith("/userinfo");
        if (userInfoEndpointRequest) {
            var clientId = context.getClient().getClientId();
            token.getOtherClaims().put("acme-userdata",
//                    Stream.iterate(1, i -> i + 1).limit(100).map(i -> "User Data: " + i)
                    List.of(1, 2, 3)
            );
        }
    }

}
