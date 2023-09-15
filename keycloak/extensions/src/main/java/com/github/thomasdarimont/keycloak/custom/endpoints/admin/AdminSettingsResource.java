package com.github.thomasdarimont.keycloak.custom.endpoints.admin;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.forms.login.freemarker.model.RealmBean;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.theme.FolderTheme;
import org.keycloak.theme.Theme;
import org.keycloak.theme.freemarker.FreeMarkerProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Renders a simple form to manage custom realm attributes.
 */
@JBossLog
public class AdminSettingsResource {

    private static final File KEYCLOAK_CUSTOM_ADMIN_THEME_FOLDER;

    static {
        try {
            File themesFolder = new File(System.getProperty("kc.home.dir"), "themes").getCanonicalFile();
            KEYCLOAK_CUSTOM_ADMIN_THEME_FOLDER = new File(themesFolder, "admin-custom/admin").getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final KeycloakSession session;
    private final AuthenticationManager.AuthResult authResult;

    public AdminSettingsResource(KeycloakSession session, AuthenticationManager.AuthResult authResult) {
        this.session = session;
        this.authResult = authResult;
    }

    @GET
    public Response adminUi() throws Exception {
        var freeMarker = session.getProvider(FreeMarkerProvider.class);
        var theme = new FolderTheme(KEYCLOAK_CUSTOM_ADMIN_THEME_FOLDER, "admin-custom", Theme.Type.ADMIN);
        var context = session.getContext();
        var realm = context.getRealm();
        var attributes = new HashMap<String, Object>();
        attributes.put("realm", new RealmBean(realm));
        attributes.put("realmSettings", new RealmSettingsBean(realm));
        attributes.put("properties", theme.getProperties());
        var baseUri = context.getUri().getBaseUriBuilder().build();
        attributes.put("url", new UrlBean(realm, theme, baseUri, null));
        var htmlString = freeMarker.processTemplate(attributes, "admin-settings.ftl", theme);
        return Response.ok().type(MediaType.TEXT_HTML_TYPE).entity(htmlString).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateAdminSettings(@Context HttpRequest request) throws Exception {

        var formData = request.getDecodedFormParameters();
        var action = formData.getFirst("action");
        if (!"save".equals(action)) {
            return adminUi();
        }

        var context = session.getContext();
        var realm = context.getRealm();

        var realmSettings = new RealmSettingsBean(realm);

        var updateCount = 0;
        for (var setting : realmSettings.getSettings()) {
            var newValue = formData.getFirst(setting.getName());
            var oldValue = setting.getValue();
            if (!Objects.equals(newValue, oldValue)) {
                realm.setAttribute(setting.getName(), newValue);
                updateCount++;
            }
        }

        if (updateCount > 0) {
            log.infof("Realm Settings updated. realm=%s user=%s", realm.getName(), authResult.getUser().getUsername());
        }

        return adminUi();
    }

    @RequiredArgsConstructor
    public static class RealmSettingsBean {

        private final RealmModel realm;

        public Map<String, String> getAttributes() {
            return realm.getAttributes();
        }

        public List<ConfigSetting> getSettings() {
            return getRawConfigSettings(setting -> {
                return setting.getName().startsWith("acme");
            });
        }

        private List<ConfigSetting> getRawConfigSettings(Predicate<ConfigSetting> filter) {

            var settings = new ArrayList<ConfigSetting>();

            for (var entry : getAttributes().entrySet()) {
                settings.add(new ConfigSetting(entry.getKey(), entry.getValue(), "text"));
            }

            settings.removeIf(Predicate.not(filter));

            return settings;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigSetting {

        String name;

        String value;

        String type;
    }
}
