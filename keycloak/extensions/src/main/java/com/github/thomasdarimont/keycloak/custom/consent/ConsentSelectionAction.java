package com.github.thomasdarimont.keycloak.custom.consent;

import com.google.auto.service.AutoService;
import lombok.Builder;
import lombok.Data;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.DisplayTypeRequiredActionFactory;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.IDToken;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@AutoService(RequiredActionFactory.class)
public class ConsentSelectionAction implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    private static final Map<String, List<ScopeField>> SCOPE_FIELD_MAPPING;

    static {
        var map = new HashMap<String, List<ScopeField>>();

        map.put(OAuth2Constants.SCOPE_PHONE, List.of(new ScopeField(IDToken.PHONE_NUMBER, "tel", u -> u.getFirstAttribute(IDToken.PHONE_NUMBER)))); //
        map.put(OAuth2Constants.SCOPE_EMAIL, List.of(new ScopeField(IDToken.EMAIL, "email", UserModel::getEmail))); //
        // TODO add dedicated client scope of name
        map.put("name", List.of( //
                new ScopeField(IDToken.GIVEN_NAME, "text", UserModel::getFirstName), //
                new ScopeField(IDToken.FAMILY_NAME, "text", UserModel::getLastName) //
        ));

        SCOPE_FIELD_MAPPING = Collections.unmodifiableMap(map);
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        // whether we can refer to that action via kc_actions URL parameter
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

        var authSession = context.getAuthenticationSession();
        var user = context.getUser();

        if ("checked".equals(authSession.getClientNote(getId()))) {
            return;
        }

        var missingConsents = getScopeInfo(context.getSession(), authSession, user);

        var prompt = context.getUriInfo().getQueryParameters().getFirst("prompt");
        var explicitConsentRequested = OIDCLoginProtocol.PROMPT_VALUE_CONSENT.equals(prompt);

        if (!missingConsents.getMissingRequired().isEmpty() || explicitConsentRequested) {
            user.addRequiredAction(getId());
            authSession.setClientNote(getId(), "checked");
        } else {
            user.removeRequiredAction(getId());
        }
    }

    private ScopeInfo getScopeInfo(KeycloakSession session, AuthenticationSessionModel authSession, UserModel user) {
        var client = authSession.getClient();
        var requestedScopes = computeRequestedScopes(authSession, client);
        var consentByClient = session.users().getConsentByClient(authSession.getRealm(), user.getId(), client.getId());
        var missingRequired = new HashSet<>(requestedScopes.getRequired().values());
        var missingOptional = new HashSet<>(requestedScopes.getOptional().values());

        var grantedRequired = Collections.<ClientScopeModel>emptySet();
        var grantedOptional = Collections.<ClientScopeModel>emptySet();

        if (consentByClient != null) {

            grantedRequired = new HashSet<>(requestedScopes.getRequired().values());
            grantedOptional = new HashSet<>(requestedScopes.getOptional().values());

            grantedRequired.retainAll(consentByClient.getGrantedClientScopes());
            grantedOptional.retainAll(consentByClient.getGrantedClientScopes());
            missingRequired.removeAll(consentByClient.getGrantedClientScopes());
            missingOptional.removeAll(consentByClient.getGrantedClientScopes());
        }

        return ScopeInfo.builder() //
                .grantedRequired(grantedRequired) //
                .grantedOptional(grantedOptional) //
                .missingRequired(missingRequired) //
                .missingOptional(missingOptional) //
                .build();
    }

    @Data
    @Builder
    static class ScopeInfo {
        private final Set<ClientScopeModel> grantedRequired;
        private final Set<ClientScopeModel> grantedOptional;

        private final Set<ClientScopeModel> missingRequired;
        private final Set<ClientScopeModel> missingOptional;
    }

    private RequestedScopes computeRequestedScopes(AuthenticationSessionModel authSession, ClientModel client) {
        var defaultClientScopes = client.getClientScopes(true);
        var optionalClientScopes = client.getClientScopes(false);

        var requestedRequired = new HashMap<String, ClientScopeModel>();
        var requestedOptional = new HashMap<String, ClientScopeModel>();
        for (var scopeId : authSession.getClientScopes()) {
            var foundInDefaultScope = false;
            for (var scope : defaultClientScopes.values()) {
                if (scope.getId().equals(scopeId)) {
                    requestedRequired.put(scope.getName(), scope);
                    foundInDefaultScope = true;
                    break;
                }
            }
            if (!foundInDefaultScope) {
                for (var scope : optionalClientScopes.values()) {
                    if (scope.getId().equals(scopeId)) {
                        requestedOptional.put(scope.getName(), scope);
                        break;
                    }
                }
            }
        }

        return new RequestedScopes(requestedRequired, requestedOptional);
    }

    @Data
    static class RequestedScopes {

        private final Map<String, ClientScopeModel> required;
        private final Map<String, ClientScopeModel> optional;

    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {

        // Show form
        context.challenge(createForm(context, null));
    }

    protected Response createForm(RequiredActionContext context, Consumer<LoginFormsProvider> formCustomizer) {

        var form = context.form();
        var user = context.getUser();
        form.setAttribute("username", user.getUsername());

        var authSession = context.getAuthenticationSession();

        Function<ScopeField, ScopeFieldBean> fun = f -> new ScopeFieldBean(f, user);

        var scopeInfo = getScopeInfo(context.getSession(), authSession, user);
        var grantedRequired = scopeInfo.getGrantedRequired();
        var missingRequired = scopeInfo.getMissingRequired();
        var grantedOptional = scopeInfo.getGrantedOptional();
        var missingOptional = scopeInfo.getMissingOptional();

        var scopes = new ArrayList<ScopeBean>();
        for (var currentScopes : List.of(grantedRequired, missingRequired, grantedOptional, missingOptional)) {
            for (var scope : currentScopes) {
                var fields = SCOPE_FIELD_MAPPING.getOrDefault(scope.getName(), List.of()).stream().map(fun).collect(toList());
                var optional = currentScopes == grantedOptional || currentScopes == missingOptional;
                var granted = currentScopes == grantedRequired || currentScopes == grantedOptional;
                scopes.add(new ScopeBean(scope, optional, granted, fields));
            }
        }

        scopes.sort(Comparator.comparing(s -> {
            String guiOrder = s.getScopeModel().getGuiOrder();
            if (guiOrder == null) {
                return s.getName();
            }
            return guiOrder;
        }));

        form.setAttribute("scopes", scopes);

        if (formCustomizer != null) {
            formCustomizer.accept(form);
        }

        // use form from src/main/resources/theme-resources/templates/
        return form.createForm("select-consent-form.ftl");
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // handle consent selection from user

        var authSession = context.getAuthenticationSession();
        var user = context.getUser();

        var formParameters = context.getHttpRequest().getFormParameters();
        var scopeSelection = formParameters.get("scopeSelection");

        var scopeInfo = getScopeInfo(context.getSession(), authSession, user);

        var scopesToAskForConsent = new HashSet<ClientScopeModel>();

        for (var scopes : List.of( //
                scopeInfo.getGrantedRequired(), //
                scopeInfo.getMissingRequired(), //
                scopeInfo.getGrantedOptional(), //
                scopeInfo.getMissingOptional())) {
            for (var scope : scopes) {
                if (scopeSelection.contains(scope.getName())) {
                    scopesToAskForConsent.add(scope);
                }
            }
        }

        if (!scopesToAskForConsent.isEmpty()) {
            var client = authSession.getClient();

            // TODO find a way to merge the existing consent with the new consent
            var consentByClient = context.getSession().users().getConsentByClient(context.getRealm(), user.getId(), client.getId());
            if (consentByClient != null) {
                context.getSession().users().revokeConsentForClient(context.getRealm(), user.getId(), client.getId());
            }
            consentByClient = new UserConsentModel(client);

            scopesToAskForConsent.forEach(consentByClient::addGrantedClientScope);

            context.getSession().users().addConsent(context.getRealm(), user.getId(), consentByClient);

            var grantedScopeNames = consentByClient.getGrantedClientScopes().stream().map(ClientScopeModel::getName).collect(Collectors.toSet());
            var scope = OAuth2Constants.SCOPE_OPENID + " " + String.join(" ", grantedScopeNames);
            authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

            context.getEvent().client(client).user(user).event(EventType.GRANT_CONSENT).detail(OAuth2Constants.SCOPE, scope).success();
        }

        // TODO ensure that required scopes are always consented
        user.removeRequiredAction(getId());
        context.success();
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        return create(session);
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

    @Override
    public String getId() {
        return "acme-dynamic-consent";
    }

    @Override
    public String getDisplayText() {
        return "Acme: Dynamic Consent selection";
    }
}
