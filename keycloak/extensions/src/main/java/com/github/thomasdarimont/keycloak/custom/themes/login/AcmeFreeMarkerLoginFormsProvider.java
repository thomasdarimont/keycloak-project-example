package com.github.thomasdarimont.keycloak.custom.themes.login;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.AuthenticationContextBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;

import javax.ws.rs.core.Response;
import java.util.Locale;

/**
 * Custom {@link FreeMarkerLoginFormsProvider} to adjust the login form rendering context.
 */
@JBossLog
public class AcmeFreeMarkerLoginFormsProvider extends FreeMarkerLoginFormsProvider {

    public AcmeFreeMarkerLoginFormsProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        super(session, freeMarker);
    }

    @Override
    protected Response processTemplate(Theme theme, String templateName, Locale locale) {
        // expose custom objects in the template rendering via super.attributes

        AuthenticationContextBean authBean = (AuthenticationContextBean)attributes.get("auth");
        attributes.put("acme", new AcmeLoginBean(session, authBean));

        return super.processTemplate(theme, templateName, locale);
    }
}
