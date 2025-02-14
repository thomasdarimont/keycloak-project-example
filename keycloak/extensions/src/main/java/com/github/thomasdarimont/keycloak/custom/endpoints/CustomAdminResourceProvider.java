package com.github.thomasdarimont.keycloak.custom.endpoints;

import com.github.thomasdarimont.keycloak.custom.endpoints.admin.CustomAdminResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.admin.UserProvisioningResource.UserProvisioningConfig;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import java.util.regex.Pattern;

@JBossLog
public class CustomAdminResourceProvider implements AdminRealmResourceProvider {

    public static final String ID = "custom-admin-resources";

    private final UserProvisioningConfig privisioningConfig;

    public CustomAdminResourceProvider(UserProvisioningConfig privisioningConfig) {
        this.privisioningConfig = privisioningConfig;
    }

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        return new CustomAdminResource(session, realm, auth, adminEvent, privisioningConfig);
    }

    @Override
    public void close() {

    }

    @AutoService(AdminRealmResourceProviderFactory.class)
    public static class Factory implements AdminRealmResourceProviderFactory {

        @Override
        public String getId() {
            return ID;
        }

        private CustomAdminResourceProvider customAdminResource;

        @Override
        public AdminRealmResourceProvider create(KeycloakSession session) {
            return customAdminResource;
        }

        @Override
        public void init(Config.Scope config) {
            String realmRole = config.scope("users", "provisioning").get("required-realm-role");
            String attributePatternString = config.scope("users", "provisioning").get("managed-attribute-pattern");

            var privisioningConfig = new UserProvisioningConfig(realmRole, Pattern.compile(attributePatternString));
            customAdminResource = new CustomAdminResourceProvider(privisioningConfig);
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            log.info("### Register custom admin resources");
        }

        @Override
        public void close() {

        }
    }
}
