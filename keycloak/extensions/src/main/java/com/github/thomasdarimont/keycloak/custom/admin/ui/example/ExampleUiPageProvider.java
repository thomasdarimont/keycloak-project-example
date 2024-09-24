package com.github.thomasdarimont.keycloak.custom.admin.ui.example;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ui.extend.UiPageProvider;
import org.keycloak.services.ui.extend.UiPageProviderFactory;
import org.keycloak.utils.KeycloakSessionUtil;

import java.util.List;

@JBossLog
@AutoService(UiPageProviderFactory.class)
public class ExampleUiPageProvider implements UiPageProvider, UiPageProviderFactory<ComponentModel> {

    @Override
    public String getId() {
        // Also used as lookup for messages resource bundle
        return "acme-admin-ui-example";
    }

    @Override
    public String getHelpText() {
        return "An example Admin UI Page";
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        log.infof("Create component settings %s", model);

        /*List<ComponentModel> allStoredComponents = realm
                .getComponentsStream(realm.getId(), UiPageProvider.class.getName())
                .filter(cm -> cm.getProviderId().equals(getId())).toList();

        for (ComponentModel cm : allStoredComponents) {
            log.infof("%s", cm);
        }*/
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        log.infof("Update component settings %s", newModel);
    }

    @Override
    public void preRemove(KeycloakSession session, RealmModel realm, ComponentModel model) {
        log.infof("Remove component settings %s", model);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {

        KeycloakSession keycloakSession = KeycloakSessionUtil.getKeycloakSession();
        KeycloakContext context = keycloakSession.getContext();
        RealmModel realm = context.getRealm();

        return ProviderConfigurationBuilder.create() //
                .property() //
                .name("booleanProperty") //
                .label("Boolean Property") //
                .required(true) //
                .defaultValue(true) //
                .helpText("A boolean Property") //
                .type(ProviderConfigProperty.BOOLEAN_TYPE) //
                .add() //
                .property() //
                .name("stringProperty") //
                .label("String Property") //
                .required(true) //
                .defaultValue("Default for " + realm.getName()) //
                .helpText("A String Property") //
                .type(ProviderConfigProperty.STRING_TYPE) //
                .add() //
                .build();
    }

    @Override
    public void init(Config.Scope config) {
        log.infof("Init component settings %s", config);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        log.infof("Post-init component settings %s", factory);
    }

    @Override
    public void close() {
        // NOOP
    }
}
