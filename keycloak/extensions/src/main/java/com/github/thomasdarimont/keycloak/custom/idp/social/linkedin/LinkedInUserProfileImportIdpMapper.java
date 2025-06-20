package com.github.thomasdarimont.keycloak.custom.idp.social.linkedin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.social.linkedin.LinkedInOIDCIdentityProviderFactory;
import org.keycloak.utils.KeycloakSessionUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Imports additional linkedin user profile data such as profile picture etc.
 */
@JBossLog
@AutoService(IdentityProviderMapper.class)
public class LinkedInUserProfileImportIdpMapper extends AbstractIdentityProviderMapper {

    private static final String[] COMPATIBLE_PROVIDERS = {LinkedInOIDCIdentityProviderFactory.PROVIDER_ID};

    @Override
    public String getId() {
        return "acme-idp-mapper-linkedin-user-importer";
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS.clone();
    }

    @Override
    public String getDisplayCategory() {
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Acme: LinkedIn User Importer";
    }

    @Override
    public String getHelpText() {
        return "Imports linkedin user profile data";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        log.debugf("Create User based on linkedin profile data. realm=%s userId=%s", realm.getName(), user.getId());
        updateUser(realm, user, context, Action.CREATE);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        log.debugf("Update User based on linkedin profile data. realm=%s userId=%s", realm.getName(), user.getId());
        updateUser(realm, user, context, Action.UPDATE);
    }

    enum Action {
        CREATE, UPDATE
    }

    private void updateUser(RealmModel realm, UserModel user, BrokeredIdentityContext context, Action action) {

        Map<String, Object> contextData = context.getContextData();
        if (contextData == null) {
            return;
        }

        ObjectNode userInfo = (ObjectNode) contextData.get("UserInfo");
        if (userInfo == null) {
            return;
        }
        try {
            JsonNode pictureEl = userInfo.get("picture");
            if (pictureEl != null) {
                String profilePictureUrl = pictureEl.asText();
                user.setSingleAttribute("picture", profilePictureUrl);
            }
        } catch (Exception ex) {
            log.warnf("Could not extract user profile picture from linkedin profile data. realm=%s userId=%s error=%s", realm.getName(), user.getId(), ex.getMessage());
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }
}
