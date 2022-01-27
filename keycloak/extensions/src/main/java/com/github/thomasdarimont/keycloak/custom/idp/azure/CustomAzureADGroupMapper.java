package com.github.thomasdarimont.keycloak.custom.idp.azure;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@JBossLog
@AutoService(IdentityProviderMapper.class)
public class CustomAzureADGroupMapper extends AbstractClaimMapper {

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

    public static final String PROVIDER_ID = "oidc-aad-groups-idp-mapper";

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
        return "Group Importer";
    }

    @Override
    public String getDisplayType() {
        return "AAD Groups claim to Group";
    }

    @Override
    public String getHelpText() {
        return "Assign the user to the specified group.";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        updateGroupsIfNecessary(session, realm, user, mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        updateGroupsIfNecessary(session, realm, user, mapperModel, context);
    }

    private void updateGroupsIfNecessary(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

        String aadAccessToken = ((AccessTokenResponse) context.getContextData().get(OIDCIdentityProvider.FEDERATED_ACCESS_TOKEN_RESPONSE)).getToken();


        JsonWebToken aadIdToken = (JsonWebToken) context.getContextData().get(OIDCIdentityProvider.VALIDATED_ID_TOKEN);
        if (aadIdToken == null) {
            log.errorf("Could not find validated AAD IDToken");
        }

        // extract group ids from AAD ID-Token
        List<String> assignedGroupIds = (List<String>) aadIdToken.getOtherClaims().get("groups");

        if (assignedGroupIds == null || assignedGroupIds.isEmpty()) {
            log.debugf("Could not find groups claim in AAD IDToken");
            return;
        }

        // TODO check if current user already has all assigned membership, in this case spare the graph api call

        // fetch available AAD groups via MS Graph API (and cache)
        // TODO add support for caching MS Graph API Responses
        AADGroupList aadGroupList = fetchGroupListFromMsGraphApi(session, aadAccessToken);
        if (aadGroupList == null) {
            return;
        }

        List<AADGroupInfo> aadAssignedGroups = aadGroupList.getEntries().stream().filter(g -> {
            String groupId = g.getId();
            return assignedGroupIds.contains(groupId);
        }).collect(Collectors.toList());

        for (AADGroupInfo aadGroup : aadAssignedGroups) {

            var aadGroupId = aadGroup.getId();
            var groupName = aadGroup.getDisplayName();
            var description = aadGroup.getDescription();

            Optional<GroupModel> maybeLocalGroup = realm.getGroupsStream() //
                    .filter(g -> g.getName().equals(groupName)) //
                    .findAny();

            GroupModel localGroup = maybeLocalGroup.map(existingGroup -> {

                existingGroup.setSingleAttribute("description", description);
                existingGroup.setSingleAttribute("aadGroupId", aadGroupId);
                return existingGroup;

            }).orElseGet(() -> {

                GroupModel newGroup = session.groups().createGroup(realm, groupName);
                newGroup.setSingleAttribute("description", description);
                newGroup.setSingleAttribute("aadGroupId", aadGroupId);
                return newGroup;
            });

            // let user join assigned groups if necessary
            if (!user.isMemberOf(localGroup)) {
                user.joinGroup(localGroup);
            }

            // TODO add ability to remove user from groups not listed in AAD Groups
        }
    }

    private AADGroupList fetchGroupListFromMsGraphApi(KeycloakSession session, String aadAccessToken) {

        AADGroupList aadGroupList = null;
        SimpleHttp groupsListingRequest = queryMsGraphApi(session, aadAccessToken, "/groups");

        SimpleHttp.Response response = null;
        try {
            response = groupsListingRequest.asResponse();

            if (response.getStatus() == 200) {
                aadGroupList = response.asJson(AADGroupList.class);
            } else {
                log.warnf("Failed to fetch groups via MS Graph API. Response: %s", response.getStatus());
            }
        } catch (Exception ex) {
            log.warnf(ex, "Failed to fetch groups via MS Graph API");
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.errorf(e, "Failed to close response");
                }
            }
        }

        return aadGroupList;
    }

    private SimpleHttp queryMsGraphApi(KeycloakSession session, String aadAccessToken, String requestPath) {
        var url = "https://graph.microsoft.com/v1.0" + requestPath;
        var request = SimpleHttp.doGet(url, session);
        request.auth(aadAccessToken);
        return request;
    }

    @Data
    public static class AADData {

        Map<String, Object> data = new HashMap<>();

        @JsonAnySetter
        public void setData(String key, Object value) {
            data.put(key, value);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AADGroupInfo extends AADData {

        String id;

        String displayName;

        String description;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AADGroupList extends AADData {

        @JsonProperty("value")
        List<AADGroupInfo> entries = new ArrayList<>();
    }
}
