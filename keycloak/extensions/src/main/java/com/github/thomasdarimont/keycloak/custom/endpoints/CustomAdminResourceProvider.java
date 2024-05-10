package com.github.thomasdarimont.keycloak.custom.endpoints;

import com.github.thomasdarimont.keycloak.custom.endpoints.admin.AdminSettingsResource;
import com.github.thomasdarimont.keycloak.custom.endpoints.admin.CustomDemoAdminResource;
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

@JBossLog
public class CustomAdminResourceProvider implements AdminRealmResourceProvider {

    public static final String ID = "custom-admin-resources";

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        return new CustomDemoAdminResource(session, realm, auth, adminEvent);
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

        private CustomAdminResourceProvider INSTANCE = new CustomAdminResourceProvider();

        @Override
        public AdminRealmResourceProvider create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public void init(Config.Scope config) {

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
