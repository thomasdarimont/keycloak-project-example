package com.github.thomasdarimont.keycloak.custom.themes.account;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.forms.account.AccountProvider;
import org.keycloak.forms.account.AccountProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.theme.FreeMarkerUtil;

@AutoService(AccountProviderFactory.class)
public class AcmeFreeMarkerAccountProviderFactory implements AccountProviderFactory {

    private FreeMarkerUtil freeMarker;

    @Override
    public AccountProvider create(KeycloakSession session) {
        return new AcmeFreeMarkerAccountProvider(session, freeMarker);
    }

    @Override
    public void init(Config.Scope config) {
        freeMarker = new FreeMarkerUtil();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
        freeMarker = null;
    }

    @Override
    public String getId() {
        return "freemarker";
    }

}