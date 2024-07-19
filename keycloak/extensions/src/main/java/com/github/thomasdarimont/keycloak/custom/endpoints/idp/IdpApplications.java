package com.github.thomasdarimont.keycloak.custom.endpoints.idp;

import com.github.thomasdarimont.keycloak.custom.endpoints.CorsUtils;
import com.github.thomasdarimont.keycloak.custom.support.LocaleUtils;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.OAuth2Constants;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.LoginBean;
import org.keycloak.forms.login.freemarker.model.RealmBean;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.theme.Theme;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@JBossLog
public class IdpApplications {

    private final KeycloakSession session;

    public IdpApplications(KeycloakSession session) {
        this.session = session;
    }

    @OPTIONS
    public Response getCorsOptions() {
        return withCors().add(Response.ok());
    }

    @GET
    public Response applications(@QueryParam("alias") String idpProviderAlias, @QueryParam("login_hint") String loginHint) throws IOException {

        KeycloakContext context = session.getContext();

        RealmModel realm = context.getRealm();
        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(session, realm, true);
        if (authResult == null) {
            // user is not authenticated yet redirect to login
            return redirect(context, idpProviderAlias, loginHint);
        }

        Set<String> defaultIgnoredClientIds = Set.of("account", "broker", "realm-management", "admin-cli", "security-admin-console", "idp-initiated");
        Predicate<ClientModel> clientFilter = client -> {

            if (defaultIgnoredClientIds.contains(client.getClientId())) {
                return false;
            }

            if (client.getBaseUrl() == null) {
                return false;
            }

            return true;
        };

        Locale locale = LocaleUtils.extractLocaleWithFallbackToRealmLocale(context.getHttpRequest(), realm);
        UserModel user = authResult.getUser();
        session.setAttribute(LocaleSelectorProvider.USER_REQUEST_LOCALE, locale.getLanguage());

        Theme loginTheme = session.theme().getTheme(realm.getLoginTheme(), Theme.Type.LOGIN);
        RealmBean realmBean = new RealmBean(realm);
        LoginBean loginBean = new LoginBean(new MultivaluedHashMap<>(Map.of("username", user.getUsername())));
        ApplicationsBean applicationsBean = new ApplicationsBean(realm, session, clientFilter);
        UrlBean urlBean = new UrlBean(realm, loginTheme, context.getUri().getBaseUri(), null);
        Response formResponse = session.getProvider(LoginFormsProvider.class) //
                .setAttribute("realm", realmBean) //
                .setAttribute("user", loginBean) //
                .setAttribute("application", applicationsBean) //
                .setAttribute("url", urlBean) //
                .createForm("login-applications.ftl");

        return formResponse;
    }

    /**
     * Initiate a login through the Identity provider with the given providerId and loginHint.
     *
     * @param context
     * @param providerId
     * @param loginHint
     */
    private Response redirect(KeycloakContext context, String providerId, String loginHint) {

        // adapted from org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticator.redirect
        RealmModel realm = context.getRealm();
        Optional<IdentityProviderModel> idp = realm.getIdentityProvidersStream() //
                .filter(IdentityProviderModel::isEnabled) //
                .filter(identityProvider -> Objects.equals(providerId, identityProvider.getAlias())) //
                .findFirst();

        if (idp.isEmpty()) {
            log.warnf("Identity Provider not found or not enabled for realm. realm=%s provider=%s", realm.getName(), providerId);
            return Response.status(Response.Status.BAD_REQUEST).entity("invalid IdP Alias").build();
        }

        String clientId = "idp-initiated";
        ClientModel idpInitiatedClient = realm.getClientByClientId(clientId);
        String redirectUri = Urls.realmBase(session.getContext().getUri().getBaseUri()).path("{realm}/custom-resources/idp/applications").build(realm.getName()).toString();;

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        if (authSession == null) {
            RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, true);
            authSession = rootAuthSession.createAuthenticationSession(idpInitiatedClient);
            authSession.setAction(CommonClientSessionModel.Action.AUTHENTICATE.name());
            authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            authSession.setClientNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, OAuth2Constants.CODE);
            authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
            authSession.setRedirectUri(redirectUri);
            authSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
        }
        String accessCode = new ClientSessionCode<>(session, realm, authSession).getOrGenerateCode();
        String tabId = authSession.getTabId();
        URI location = Urls.identityProviderAuthnRequest(context.getUri().getBaseUri(), providerId, realm.getName(), accessCode, clientId, tabId, null, loginHint);
        Response response = Response.seeOther(location).build();
        log.debugf("Redirecting to %s", providerId);
        return response;
    }


    private Cors withCors() {
        var request = session.getContext().getHttpRequest();
        return CorsUtils.addCorsHeaders(session, request, Set.of("GET", "OPTIONS"), null);
    }

    @Data
    @RequiredArgsConstructor
    public static class ApplicationsBean {

        private final RealmModel realm;
        private final KeycloakSession session;
        private final Predicate<ClientModel> clientFilter;

        public List<ApplicationInfo> getApplications() {
            List<ApplicationInfo> applications = session.clients().getClientsStream(realm) //
                    .filter(clientFilter == null ? c -> true : clientFilter) //
                    .map(client -> {
                        String clientId = client.getClientId();
                        String name = client.getName();
                        String description = client.getDescription();
                        if (description == null) {
                            description = "";
                        }
                        String icon = client.getAttribute("icon");

                        String clientRedirectUri = Urls.realmBase(session.getContext().getUri().getBaseUri()).path(RealmsResource.class, "getRedirect").build(realm.getName(), clientId).toString();
                        return new ApplicationInfo(clientId, name, description, icon, clientRedirectUri);
                    }).toList();
            return applications;
        }

        @Data
        @RequiredArgsConstructor
        public static class ApplicationInfo {

            private final String clientId;
            private final String name;
            private final String description;
            private final String icon;
            private final String url;
        }

    }
}
