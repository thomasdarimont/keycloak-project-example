package com.github.thomasdarimont.keycloak.custom.endpoints.applications;

import com.github.thomasdarimont.keycloak.custom.endpoints.CorsUtils;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.representations.account.ConsentRepresentation;
import org.keycloak.representations.account.ConsentScopeRepresentation;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.theme.Theme;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

public class ApplicationsInfoResource {

    private final KeycloakSession session;
    private final AccessToken token;

    @Context
    private HttpRequest request;

    public ApplicationsInfoResource(KeycloakSession session, AccessToken token) {
        this.session = session;
        this.token = token;
    }

    @OPTIONS
    public Response getCorsOptions() {
        return withCors(request, Response.ok()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readApplicationInfo(@QueryParam("name") String clientName) {

        if (token == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();
        UserModel user = session.users().getUserById(realm, token.getSubject());
        if (user == null) {
            return Response.status(UNAUTHORIZED).build();
        }

        var resourceAccess = token.getResourceAccess();
        AccessToken.Access accountAccess = resourceAccess == null ? null : resourceAccess.get(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        var canAccessAccount = accountAccess != null
                && (accountAccess.isUserInRole(AccountRoles.MANAGE_ACCOUNT) || accountAccess.isUserInRole(AccountRoles.VIEW_PROFILE));
        if (canAccessAccount) {
            return Response.status(FORBIDDEN).build();
        }

        var clientRepresentationList = getApplicationsForUser(realm, user, clientName);

        var responseBody = new HashMap<String, Object>();
        responseBody.put("clients", clientRepresentationList);

        return withCors(request, Response.ok(responseBody)).build();
    }

    public List<ClientRepresentation> getApplicationsForUser(RealmModel realm, UserModel user, String clientName) {

        List<String> inUseClients = new LinkedList<>();
        Set<ClientModel> clients = session.sessions().getUserSessionsStream(realm, user)
                .flatMap(s -> s.getAuthenticatedClientSessions().values().stream())
                .map(AuthenticatedClientSessionModel::getClient)
                .peek(client -> inUseClients.add(client.getClientId())).collect(Collectors.toSet());

        List<String> offlineClients = new LinkedList<>();
        clients.addAll(session.sessions().getOfflineUserSessionsStream(realm, user)
                .flatMap(s -> s.getAuthenticatedClientSessions().values().stream())
                .map(AuthenticatedClientSessionModel::getClient)
                .peek(client -> offlineClients.add(client.getClientId()))
                .collect(Collectors.toSet()));

        Map<String, UserConsentModel> consentModels = new HashMap<>();
        clients.addAll(session.users().getConsentsStream(realm, user.getId())
                .peek(consent -> consentModels.put(consent.getClient().getClientId(), consent))
                .map(UserConsentModel::getClient)
                .collect(Collectors.toSet()));

        realm.getAlwaysDisplayInConsoleClientsStream().forEach(clients::add);

        Locale locale = session.getContext().resolveLocale(user);
        Properties messages = getAccountMessages(locale);
        return clients.stream().filter(client -> !client.isBearerOnly() && client.getBaseUrl() != null && !client.getClientId().isEmpty())
                .filter(client -> matches(client, clientName))
                .map(client -> modelToRepresentation(client, inUseClients, offlineClients, consentModels, messages))
                .collect(Collectors.toList());
    }

    private boolean matches(ClientModel client, String name) {

        if (name == null) {
            return true;
        }

        if (client.getName() == null) {
            return false;
        }

        return client.getName().toLowerCase().contains(name.toLowerCase());
    }

    private ClientRepresentation modelToRepresentation(ClientModel model, List<String> inUseClients, List<String> offlineClients, Map<String, UserConsentModel> consents, Properties messages) {
        ClientRepresentation representation = new ClientRepresentation();
        representation.setClientId(model.getClientId());
        representation.setClientName(StringPropertyReplacer.replaceProperties(model.getName(), messages));
        representation.setDescription(model.getDescription());
        representation.setUserConsentRequired(model.isConsentRequired());
        representation.setInUse(inUseClients.contains(model.getClientId()));
        representation.setOfflineAccess(offlineClients.contains(model.getClientId()));
        representation.setRootUrl(model.getRootUrl());
        representation.setBaseUrl(model.getBaseUrl());
        representation.setEffectiveUrl(ResolveRelative.resolveRelativeUri(session, model.getRootUrl(), model.getBaseUrl()));
        UserConsentModel consentModel = consents.get(model.getClientId());
        if (consentModel != null) {
            representation.setConsent(modelToRepresentation(consentModel, messages));
        }
        return representation;
    }

    private Properties getAccountMessages(Locale locale) {
        try {
            return session.theme().getTheme(Theme.Type.ACCOUNT).getMessages(locale);
        } catch (IOException e) {
            return null;
        }
    }

    private ConsentRepresentation modelToRepresentation(UserConsentModel model, Properties messages) {
        List<ConsentScopeRepresentation> grantedScopes = model.getGrantedClientScopes().stream()
                .map(m -> new ConsentScopeRepresentation(m.getId(), m.getName(), StringPropertyReplacer.replaceProperties(m.getConsentScreenText(), messages)))
                .collect(Collectors.toList());
        return new ConsentRepresentation(grantedScopes, model.getCreatedDate(), model.getLastUpdatedDate());
    }

    private Cors withCors(HttpRequest request, Response.ResponseBuilder responseBuilder) {
        return CorsUtils.addCorsHeaders(session, request, responseBuilder, Set.of("GET", "OPTIONS"), null);
    }

}
