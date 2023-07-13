package com.github.thomasdarimont.keycloak.custom.themes.login;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.LoginFormsProviderFactory;
import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProvider;
import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProviderFactory;
import org.keycloak.forms.login.freemarker.model.AuthenticationContextBean;
import org.keycloak.forms.login.freemarker.model.ClientBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.theme.Theme;

import jakarta.ws.rs.core.Response;
import java.util.Locale;

/**
 * Custom {@link FreeMarkerLoginFormsProvider} to adjust the login form rendering context.
 */
@JBossLog
public class AcmeFreeMarkerLoginFormsProvider extends FreeMarkerLoginFormsProvider {

    public AcmeFreeMarkerLoginFormsProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    protected Response processTemplate(Theme theme, String templateName, Locale locale) {
        // expose custom objects in the template rendering via super.attributes

        var authBean = (AuthenticationContextBean) attributes.get("auth");
        attributes.put("acmeLogin", new AcmeLoginBean(session, authBean));

        var clientBean = (ClientBean) attributes.get("client");
        attributes.put("acmeUrl", new AcmeUrlBean(session, clientBean));

        // TODO remove hack for custom profile fields
        if (attributes.containsKey("customProfile")) {
            attributes.put("profile", attributes.get("customProfile"));
        }

        return super.processTemplate(theme, templateName, locale);
    }

    @AutoService(LoginFormsProviderFactory.class)
    public static class Factory extends FreeMarkerLoginFormsProviderFactory {

        @Override
        public LoginFormsProvider create(KeycloakSession session) {
            return new AcmeFreeMarkerLoginFormsProvider(session);
        }

        @Override
        public void init(Config.Scope config) {
            // NOOP
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            // NOOP
        }

        @Override
        public void close() {
            // NOOP
        }
    }
}
