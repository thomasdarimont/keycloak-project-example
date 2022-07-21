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
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@AutoService(RequiredActionFactory.class)
public class ConsentSelectionAction implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    private static final boolean REQUIRE_UPDATE_PROFILE_AFTER_CONSENT_UPDATE = false;

    private static final String AUTH_SESSION_CONSENT_CHECK_MARKER = "checked";
    public static final String DEFAULT_CLIENT = "default";

    private Map<String, List<ScopeField>> getScopeFieldMapping(String clientId) {

        // Holds the client fields mapping, LinkedHashMap to retain insertion order
        var clientToScopeFieldsMapping = new LinkedHashMap<String, List<ScopeFieldsMapping>>();
        clientToScopeFieldsMapping.put(DEFAULT_CLIENT, List.of(

                new ScopeFieldsMapping("email", List.of(new ScopeField("email", "email", UserModel::getEmail, true, true))),
                new ScopeFieldsMapping("phone", List.of(new ScopeField("phoneNumber", "tel", u -> u.getFirstAttribute("phoneNumber"),true, false))),
                new ScopeFieldsMapping("birthdate", List.of(new ScopeField("birthdate", "text", u -> u.getFirstAttribute("birthdate"),true, false))),
                new ScopeFieldsMapping("firstname", List.of(new ScopeField("firstName", "text", UserModel::getFirstName,true, false))),

                new ScopeFieldsMapping("name", List.of( //
                        new ScopeField("salutation", "text", u -> u.getFirstAttribute("salutation"),false, false), //
                        new ScopeField("title", "text", u -> u.getFirstAttribute("title"),false, false), //
                        new ScopeField("firstName", "text", UserModel::getFirstName,true, false), //
                        new ScopeField("lastName", "text", UserModel::getLastName,true, false) //
                )),

                new ScopeFieldsMapping("address", List.of( //
                        new ScopeField("address.street", "text", u -> "Have it your way 42",true, false), //
                        new ScopeField("address.careOf", "text", u -> "",false, false), //
                        new ScopeField("address.postalCode", "text", u -> "12345",true, false), //
                        new ScopeField("address.city", "text", u -> "Saarbrücken",true, false), //
                        new ScopeField("address.region", "text", u -> "Saarland",false, false), //
                        new ScopeField("address.country", "text", u -> "DE",true, false) //
                )),

                new ScopeFieldsMapping("address:billing", List.of( //
                    new ScopeField("address:billing.street", "text", u -> u.getFirstAttribute("address.street"),true, false), //
                    new ScopeField("address:billing.careOf", "text", u -> u.getFirstAttribute("address.careOf"),false, false), //
                    new ScopeField("address:billing.postalCode", "text", u -> u.getFirstAttribute("address.postalCode"),true, false), //
                    new ScopeField("address:billing.city", "text", u -> u.getFirstAttribute("address.city"),true, false), //
                    new ScopeField("address:billing.region", "text", u -> u.getFirstAttribute("address.region"),false, false), //
                    new ScopeField("address:billing.country", "text", u -> u.getFirstAttribute("address.country"),true, false) //
                ))
        ));

        var defaultScopeFieldsMappings = clientToScopeFieldsMapping.get("default");

        var map = new HashMap<String, List<ScopeField>>();

        // add default scope field mapping
        for (var scopeFieldsMapping : defaultScopeFieldsMappings) {
            var fields = map.computeIfAbsent(scopeFieldsMapping.getScope(), ignored -> new ArrayList<>());
            fields.addAll(scopeFieldsMapping.getFields());
        }

        // override default scope field mapping if necessary
        if (clientToScopeFieldsMapping.containsKey(clientId)) {
            var clientScopeFieldsMappings = clientToScopeFieldsMapping.get(clientId);
            // remove default scope fields for overridden scopes
            clientToScopeFieldsMapping.keySet().forEach(map::remove);
            for (var scopeFieldsMapping : clientScopeFieldsMappings) {
                var fields = map.computeIfAbsent(scopeFieldsMapping.getScope(), ignored -> new ArrayList<>());
                fields.addAll(scopeFieldsMapping.getFields());
            }
        }

        return Collections.unmodifiableMap(map);
    }

    @Override
    public String getId() {
        return "acme-dynamic-consent";
    }

    @Override
    public String getDisplayText() {
        return "Acme: Dynamic Consent selection";
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
    public InitiatedActionSupport initiatedActionSupport() {
        // whether we can refer to that action via kc_actions URL parameter
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

        var authSession = context.getAuthenticationSession();
        var user = context.getUser();

        // For Keycloak versions up to 18.0.2 evaluateTriggers is called multiple times,
        // since we need to perform this check only once per auth session, we use a marker
        // to remember whether the check already took place.
        if (AUTH_SESSION_CONSENT_CHECK_MARKER.equals(authSession.getClientNote(getId()))) {
            return;
        }

        var missingConsents = getScopeInfo(context.getSession(), authSession, user);

        var prompt = context.getUriInfo().getQueryParameters().getFirst(OAuth2Constants.PROMPT);
        var explicitConsentRequested = OIDCLoginProtocol.PROMPT_VALUE_CONSENT.equals(prompt);

        var consentMissingForRequiredScopes = !missingConsents.getMissingRequired().isEmpty();
        var consentMissingForOptionalScopes = !missingConsents.getMissingOptional().isEmpty();
        var consentSelectionRequired = explicitConsentRequested || consentMissingForRequiredScopes || consentMissingForOptionalScopes;
        if (consentSelectionRequired) {
            authSession.addRequiredAction(getId());
            authSession.setClientNote(getId(), AUTH_SESSION_CONSENT_CHECK_MARKER);

            if (consentMissingForRequiredScopes && REQUIRE_UPDATE_PROFILE_AFTER_CONSENT_UPDATE) {
                authSession.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
            }
        } else {
            authSession.removeRequiredAction(getId());
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {

        // Show form
        context.challenge(createForm(context, null));
    }

    protected Response createForm(RequiredActionContext context, Consumer<LoginFormsProvider> formCustomizer) {

        var form = context.form();
        var user = context.getUser();

        form.setAttribute(UserModel.USERNAME, user.getUsername());

        var authSession = context.getAuthenticationSession();

        Function<ScopeField, ScopeFieldBean> fun = f -> new ScopeFieldBean(f, user);

        var scopeInfo = getScopeInfo(context.getSession(), authSession, user);
        var grantedRequired = scopeInfo.getGrantedRequired();
        var grantedOptional = scopeInfo.getGrantedOptional();
        var missingRequired = scopeInfo.getMissingRequired();
        var missingOptional = scopeInfo.getMissingOptional();
        var clientId = authSession.getClient().getClientId();
        var scopeFieldMapping = getScopeFieldMapping(clientId);
        var scopes = new ArrayList<ScopeBean>();
        for (var currentScopes : List.of(grantedRequired, missingRequired, grantedOptional, missingOptional)) {
            for (var scope : currentScopes) {

                var fields = scopeFieldMapping.getOrDefault(scope.getName(), List.of()).stream().map(fun).collect(toList());
                var optional = currentScopes == grantedOptional || currentScopes == missingOptional;
                var granted = currentScopes == grantedRequired || currentScopes == grantedOptional;
                scopes.add(new ScopeBean(scope, optional, granted, fields));
            }
        }

        scopes.sort(ScopeBean.DEFAULT_ORDER);

        try {
            System.out.printf("Scope Profile Field Mapping: %s%n", JsonSerialization.writeValueAsString(scopes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        form.setAttribute("scopes", scopes);

        form.setAttribute("grantedScopeNames", scopeInfo.getGrantedScopeNames());

        form.setAttribute("grantedScope", String.join(" ", scopeInfo.getGrantedScopeNames()));
        form.setAttribute("requestedScope", String.join(" ", scopeInfo.getRequestedScopeNames()));

        if (formCustomizer != null) {
            formCustomizer.accept(form);
        }

        // use form from src/main/resources/theme-resources/templates/
        return form.createForm("select-consent-form.ftl");
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // handle consent selection from user
        var formParameters = context.getHttpRequest().getFormParameters();

        var authSession = context.getAuthenticationSession();
        var session = context.getSession();
        var users = context.getSession().users();
        var realm = context.getRealm();
        var event = context.getEvent();
        var client = authSession.getClient();
        var user = context.getUser();

        event.client(client).user(user).event(EventType.GRANT_CONSENT);

        if (formParameters.getFirst("cancel") != null) {
            // User choose NOT to update consented scopes

            event.error(Errors.CONSENT_DENIED);
            // return to the application without consent update
            UserConsentModel consentModel = session.users().getConsentByClient(realm, user.getId(), client.getId());
            if (consentModel == null) {
                // No consents given: Deny access to application
                context.failure();
                return;
            }

            var currentGrantedScopes = consentModel.getGrantedClientScopes();
            if (currentGrantedScopes.isEmpty()) {
                // No consents given: Deny access to application
                context.failure();
                return;
            }

            var currentGrantedScopesIds = currentGrantedScopes.stream().map(ClientScopeModel::getId).collect(Collectors.toSet());
            var currentGrantedScopeNames = currentGrantedScopes.stream().map(ClientScopeModel::getName).collect(Collectors.joining(" "));
            context.getAuthenticationSession().setClientScopes(currentGrantedScopesIds);
            context.getAuthenticationSession().setClientNote(OAuth2Constants.SCOPE, "openid " + currentGrantedScopeNames);

            // Allow access to application (with original consented scopes)
            context.success();
            return;
        }

        var scopeSelection = formParameters.get("scopeSelection");
        var scopeInfo = getScopeInfo(session, authSession, user);
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
            // TODO find a way to merge the existing consent with the new consent instead of replacing the existing consent
            var consentByClient = users.getConsentByClient(realm, user.getId(), client.getId());
            if (consentByClient != null) {
                users.revokeConsentForClient(realm, user.getId(), client.getId());
            }
            consentByClient = new UserConsentModel(client);

            scopesToAskForConsent.forEach(consentByClient::addGrantedClientScope);

            users.addConsent(realm, user.getId(), consentByClient);

            var grantedScopeNames = consentByClient.getGrantedClientScopes().stream().map(ClientScopeModel::getName).collect(Collectors.toList());
            grantedScopeNames.add(0, OAuth2Constants.SCOPE_OPENID);
            var scope = String.join(" ", grantedScopeNames);

            // TODO find a better way to propagate the selected scopes
            authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

            event.detail(OAuth2Constants.SCOPE, scope).success();
        }

        // TODO ensure that required scopes are always consented
        authSession.removeRequiredAction(getId());
        context.success();
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
    @Builder
    static class ScopeInfo {

        private final Set<ClientScopeModel> grantedRequired;
        private final Set<ClientScopeModel> grantedOptional;
        private final Set<ClientScopeModel> missingRequired;
        private final Set<ClientScopeModel> missingOptional;

        private Set<String> getGrantedScopeNames() {
            var result = new LinkedHashSet<String>();
            grantedRequired.stream().map(ClientScopeModel::getName).forEach(result::add);
            grantedOptional.stream().map(ClientScopeModel::getName).forEach(result::add);
            return result;
        }

        private Set<String> getRequestedScopeNames() {
            var result = new LinkedHashSet<String>();
            missingRequired.stream().map(ClientScopeModel::getName).forEach(result::add);
            missingOptional.stream().map(ClientScopeModel::getName).forEach(result::add);
            return result;
        }
    }

    @Data
    static class RequestedScopes {

        private final Map<String, ClientScopeModel> required;
        private final Map<String, ClientScopeModel> optional;
    }

    @Data
    static class ScopeFieldsMapping {

        private final String scope;

        private final List<ScopeField> fields;
    }
}
