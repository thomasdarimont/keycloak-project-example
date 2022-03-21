package com.github.thomasdarimont.keycloak.custom.oidc.remoteclaims;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.thomasdarimont.keycloak.custom.support.TokenUtils;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.apache.http.HttpHeaders;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
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
import org.keycloak.services.Urls;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>{@code
 *
 * KC_ISSUER=http://localhost:8081/auth/realms/remote-claims
 * KC_CLIENT_ID=demo-client-remote-claims
 * KC_USERNAME=tester
 * KC_PASSWORD=test
 *
 * KC_RESPONSE=$( \
 * curl \
 *   -d "client_id=$KC_CLIENT_ID" \
 *   -d "username=$KC_USERNAME" \
 *   -d "password=$KC_PASSWORD" \
 *   -d "grant_type=password" \
 *   "$KC_ISSUER/protocol/openid-connect/token" \
 * )
 * echo $KC_RESPONSE | jq -C .
 *
 * }</pre>
 */
@JBossLog
@AutoService(ProtocolMapper.class)
public class RemoteOidcMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final String PROVIDER_ID = "oidc-remote-protocol-mapper";

    private static final Logger LOGGER = Logger.getLogger(RemoteOidcMapper.class);

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    private static final String CONFIG_REMOTE_URL = "remoteUrl";

    private static final String CONFIG_ADD_AUTH_HEADER = "addAuthHeader";

    public static final String DEFAULT_REMOTE_CLAIM_URL = "https://id.acme.test:4543/api/users/claims?userId={userId}&username={username}&clientId={clientId}&issuer={issuer}";

    public static final String ROOT_OBJECT = "$ROOT$";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {

        CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
                .property()
                .name(CONFIG_REMOTE_URL)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Remote URL")
                .helpText("URL to fetch custom claims for the given user")
                .defaultValue(DEFAULT_REMOTE_CLAIM_URL)
                .add()

                .property()
                .name(CONFIG_ADD_AUTH_HEADER)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Add Authorization Header")
                .helpText("If set to true, an dynamically generated access-token will added to the Authorization header of the request")
                .defaultValue(false)
                .add()

                .build();

        OIDCAttributeMapperHelper.addAttributeConfig(CONFIG_PROPERTIES, UserPropertyMapper.class);
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Demo Remote Mapper";
    }

    @Override
    public String getHelpText() {
        return "A protocol mapper that can fetch additional claims from an external service";
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

        // extract information from httpRequest as necessary
//        HttpRequest httpRequest = Resteasy.getContextData(HttpRequest.class);
//        httpRequest.getFormParameters().getFirst("formParam");
//        httpRequest.getUri().getQueryParameters().getFirst("queryParam")


        KeycloakContext context = session.getContext();
        boolean userInfoEndpointRequest = context.getUri().getPath().endsWith("/userinfo");

        String issuer = token.getIssuedFor();
        String clientId = token.getIssuedFor();
        if (userInfoEndpointRequest) {
            clientId = context.getClient().getClientId();
            issuer = Urls.realmIssuer(context.getUri().getBaseUri(), context.getRealm().getName());
        }

        Object claimValue = fetchRemoteClaims(mappingModel, userSession, session, issuer, clientId);
        LOGGER.infof("setClaim %s=%s", mappingModel.getName(), claimValue);

        String claimName = mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        if (copyClaimsToRoot(claimValue, claimName)) {
            Map<String, Object> values = MAPPER.convertValue(claimValue, new TypeReference<>() {
            });
            token.getOtherClaims().putAll(values);
            return;
        }

        if (claimValue == null) {
            log.warnf("Remote claims request returned null.");
            return;
        }

        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, claimValue);
    }

    private boolean copyClaimsToRoot(Object claimValue, String claimName) {
        return ROOT_OBJECT.equals(claimName) && claimValue instanceof ObjectNode;
    }

    private Object fetchRemoteClaims(ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession session, String issuer, String clientId) {

        try {
            String url = createUri(mappingModel, userSession, issuer, clientId);
            SimpleHttp http = SimpleHttp.doGet(url, session);

            addAuthHeaderIfNecessary(mappingModel, http, userSession, session);

            SimpleHttp.Response response = http.asResponse();

            if (response.getStatus() != 200) {
                log.warnf("Could not fetch remote claims for user. status=%s", response.getStatus());
                return null;
            }

            return response.asJson();
        } catch (IOException e) {
            log.warnf("Could not fetch remote claims for user. error=%s", e.getMessage());
        }

        return null;
    }

    protected void addAuthHeaderIfNecessary(ProtocolMapperModel mappingModel, SimpleHttp http, UserSessionModel userSession, KeycloakSession session) {

        if (!Boolean.parseBoolean(mappingModel.getConfig().getOrDefault(CONFIG_ADD_AUTH_HEADER, "false"))) {
            return;
        }

        String accessToken = TokenUtils.generateAccessToken(session, userSession, "admin-cli", "iam", token -> {
            // mark this token request as an internal iam request
            token.getOtherClaims().put("groups", List.of("iam"));
        });
        http.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    protected String createUri(ProtocolMapperModel mappingModel, UserSessionModel userSession, String issuer, String clientId) {

        String remoteUrlTemplate = mappingModel.getConfig().getOrDefault(CONFIG_REMOTE_URL, DEFAULT_REMOTE_CLAIM_URL);
        UserModel user = userSession.getUser();
        UriBuilder uriBuilder = UriBuilder.fromUri(remoteUrlTemplate);

        Map<String, Object> params = new HashMap<>();
        params.put("userId", user.getId());
        params.put("username", user.getUsername());
        params.put("clientId", clientId);
        params.put("issuer", issuer);

        URI uri = uriBuilder.buildFromMap(params);
        return uri.toString();
    }
}