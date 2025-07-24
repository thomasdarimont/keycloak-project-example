package com.github.thomasdarimont.keycloak.custom.idp.azure;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.google.auto.service.AutoService;
import lombok.Data;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EntraID Profile Mapper
 */
@JBossLog
@AutoService(IdentityProviderMapper.class)
public class CustomEntraIdProfileMapper extends AbstractClaimMapper {

    public static final String[] COMPATIBLE_PROVIDERS = {OIDCIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> configProperties;

    static {

        var properties = new ArrayList<ProviderConfigProperty>();

//        var claimsProperty = new ProviderConfigProperty();
//        claimsProperty.setName(CLAIM_PROPERTY_NAME);
//        claimsProperty.setLabel("Claims");
//        claimsProperty.setHelpText("Name and value of the claims to search for in token. You can reference nested claims using a '.', i.e. 'address.locality'. To use dot (.) literally, escape it with backslash (\\.)");
//        claimsProperty.setType(ProviderConfigProperty.MAP_TYPE);
//        configProperties.add(claimsProperty);

        configProperties = Collections.unmodifiableList(properties);
    }

    public static final String PROVIDER_ID = "oidc-aad-profile-idp-mapper";

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Profile";
    }

    @Override
    public String getDisplayType() {
        return "Acme: EntraID Profile Mapper";
    }

    @Override
    public String getHelpText() {
        return "Fetch additional profile from EntraID.";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        updateProfileIfNecessary(session, realm, user, mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        updateProfileIfNecessary(session, realm, user, mapperModel, context);
    }

    private void updateProfileIfNecessary(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

        JsonWebToken aadIdToken = (JsonWebToken) context.getContextData().get(OIDCIdentityProvider.VALIDATED_ID_TOKEN);
        if (aadIdToken == null) {
            log.errorf("Could not find validated AAD IDToken");
            return;
        }

        if (aadIdToken.getOtherClaims() == null) {
            log.errorf("Could not find additional claims in AAD IDToken");
            return;
        }

//        log.info("Fetching profile...");
//        String aadAccessToken = ((AccessTokenResponse) context.getContextData().get(OIDCIdentityProvider.FEDERATED_ACCESS_TOKEN_RESPONSE)).getToken();
//        GraphApiData graphApiData = fetchProfileFromMsGraphApi(session, aadAccessToken);
//        log.infof("Fetched profile successfully.");
//
//        updatePhoneInformation(user, graphApiData);

        updateLocaleInformation(realm, user, context);
    }

    protected void updateLocaleInformation(RealmModel realm, UserModel user, BrokeredIdentityContext context) {
        var idToken = (JsonWebToken) context.getContextData().get(OIDCIdentityProvider.VALIDATED_ID_TOKEN);

        String userPreferedLang = (String)idToken.getOtherClaims().get("xms_pl");

        if (userPreferedLang != null) {
            user.setSingleAttribute("locale", userPreferedLang);
            return;
        }

        String tenantPreferedLang = (String)idToken.getOtherClaims().get("xms_tpl");
        if (tenantPreferedLang != null) {
            user.setSingleAttribute("locale", tenantPreferedLang);
            return;
        }

        // fallback to default
        user.setSingleAttribute("locale", realm.getDefaultLocale());
    }

    protected void updatePhoneInformation(UserModel user, GraphApiData graphApiData) {
        for (var phone : graphApiData.getPhones()) {
            switch (phone.getType()) {
                case "mobile":
                    user.setSingleAttribute("phone_number", phone.getNumber());
                    user.setSingleAttribute("phone_number_verified", "true");
                    break;
                case "business":
                    user.setSingleAttribute("business_phone_number", phone.getNumber());
                    user.setSingleAttribute("business_phone_number_verified", "true");
                    break;
            }
        }
    }

    private GraphApiData fetchProfileFromMsGraphApi(KeycloakSession session, String aadAccessToken) {

        GraphApiData graphApiData = null;
        SimpleHttp groupsListingRequest = queryMsGraphApi(session, aadAccessToken, "/beta/me/profile/");

        try (SimpleHttp.Response response = groupsListingRequest.asResponse()) {

            if (response.getStatus() == 200) {
                graphApiData = response.asJson(GraphApiData.class);
            } else {
                log.warnf("Failed to fetch MS Graph API. Response: %s", response.getStatus());
            }
        } catch (Exception ex) {
            log.warnf(ex, "Failed to fetch MS Graph API");
        }

        return graphApiData;
    }

    private SimpleHttp queryMsGraphApi(KeycloakSession session, String aadAccessToken, String requestPath) {
        var url = "https://graph.microsoft.com" + requestPath;
        var request = SimpleHttp.doGet(url, session);
        request.auth(aadAccessToken);
        return request;
    }

    @Data
    public static class GraphApiData {

        Map<String, Object> data = new HashMap<>();

        List<EntraIdPhone> phones = new ArrayList<>();

        @JsonAnySetter
        public void setData(String key, Object value) {
            data.put(key, value);
        }
    }

    @Data
    public static class EntraIdPhone {
        String type;
        String number;

        Map<String, Object> data = new HashMap<>();

        @JsonAnySetter
        public void setData(String key, Object value) {
            data.put(key, value);
        }
    }
}
