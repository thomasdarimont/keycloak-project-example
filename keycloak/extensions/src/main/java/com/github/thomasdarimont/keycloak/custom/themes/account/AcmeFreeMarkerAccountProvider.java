package com.github.thomasdarimont.keycloak.custom.themes.account;

import com.google.auto.service.AutoService;
import freemarker.template.TemplateMethodModelEx;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.forms.account.AccountPages;
import org.keycloak.forms.account.AccountProvider;
import org.keycloak.forms.account.AccountProviderFactory;
import org.keycloak.forms.account.freemarker.FreeMarkerAccountProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.services.Urls;
import org.keycloak.theme.FreeMarkerUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

public class AcmeFreeMarkerAccountProvider extends FreeMarkerAccountProvider {

    public AcmeFreeMarkerAccountProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        super(session, freeMarker);
    }

    @Override
    public Response createResponse(AccountPages page) {

        if (attributes == null) {
            attributes = new HashMap<>();
        }

        if (!attributes.containsKey("acmeAccountRequiredActionUrl")) {
            // ensure required actions formatter is present
            attributes.put("acmeAccountRequiredActionUrl", new AcmeRequiredActionUrlFormatterMethod(realm, uriInfo.getBaseUri()));
        }

        Response response = super.createResponse(page);
        return response;
    }

    static class AcmeRequiredActionUrlFormatterMethod implements TemplateMethodModelEx {

        private final String realm;
        private final URI baseUri;

        public AcmeRequiredActionUrlFormatterMethod(RealmModel realm, URI baseUri) {
            this.realm = realm.getName();
            this.baseUri = baseUri;
        }

        @Override
        public Object exec(List list) {
            String action = list.get(0).toString();

            String authPath = "/auth/realms/" + realm + "/protocol/openid-connect/auth";
            String redirectUri = Urls.accountBase(baseUri).build(realm).toString();

            String url = UriBuilder.fromUri(baseUri).replacePath(authPath).queryParam(Constants.KC_ACTION, action)
                    .queryParam(Constants.CLIENT_ID, "account")
                    .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                    .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                    .build().toString();

            return url;
        }
    }

    @AutoService(AccountProviderFactory.class)
    public static class Factory implements AccountProviderFactory {

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
}
