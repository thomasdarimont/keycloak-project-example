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
import org.keycloak.models.utils.FormMessage;
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
    public static final String CONSENTED_FIELD_LIST = "CONSENTED_FIELD_LIST";

    private Map<String, List<ProfileAttribute>> getScopeFieldMapping(String clientId) {

        // Holds the client fields mapping, LinkedHashMap to retain insertion order
        var clientToScopeFieldsMapping = new LinkedHashMap<String, List<ScopeFieldsMapping>>();
        clientToScopeFieldsMapping.put(DEFAULT_CLIENT, List.of(

                new ScopeFieldsMapping("email", List.of(new KeycloakProfileAttribute("email", "email", "email", true, true, UserModel::getEmail))),
                new ScopeFieldsMapping("phone", List.of(new KeycloakProfileAttribute("phoneNumber", "phone_number", "tel", true, false, u -> u.getFirstAttribute("phoneNumber")))),
                new ScopeFieldsMapping("birthdate", List.of(new KeycloakProfileAttribute("birthdate", "birthdate", "text", true, false, u -> u.getFirstAttribute("birthdate")))),
                new ScopeFieldsMapping("firstname", List.of(new KeycloakProfileAttribute("firstName", "given_name", "text", true, false, UserModel::getFirstName))),

                new ScopeFieldsMapping("name", List.of( //
                        new KeycloakProfileAttribute("salutation", "salutation", "text", false, false, u -> u.getFirstAttribute("salutation")), //
                        new KeycloakProfileAttribute("title", "title", "text", false, false, u -> u.getFirstAttribute("title")), //
                        new KeycloakProfileAttribute("firstName", "given_name", "text", true, false, UserModel::getFirstName), //
                        new KeycloakProfileAttribute("lastName", "family_name", "text", true, false, UserModel::getLastName) //
                )),

                new ScopeFieldsMapping("address", List.of( //
                        new KeycloakProfileAttribute("address.street", "address.street", "text", true, false, u -> "Have it your way 42"), //
                        new KeycloakProfileAttribute("address.careOf", "address.careOf", "text", false, false, u -> ""), //
                        new KeycloakProfileAttribute("address.postalCode", "address.postalCode", "text", true, false, u -> "12345"), //
                        new KeycloakProfileAttribute("address.city", "address.city", "text", true, false, u -> "Saarbrücken"), //
                        new KeycloakProfileAttribute("address.region", "address.region", "text", false, false, u -> "Saarland"), //
                        new KeycloakProfileAttribute("address.country", "address.country", "text", true, false, u -> "DE") //
                ))
        ));

        var defaultScopeFieldsMappings = clientToScopeFieldsMapping.get("default");

        var map = new HashMap<String, List<ProfileAttribute>>();

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

        if (!isDynamicConsentManagementEnabled(authSession.getClient())) {
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

    private boolean isDynamicConsentManagementEnabled(ClientModel client) {
        return Set.of("app-greetme", "app-consent-demo").contains(client.getClientId());
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
        var session = context.getSession();

        Function<ProfileAttribute, ScopeFieldBean> fun = f -> new ScopeFieldBean(f, user);

        var scopeInfo = getScopeInfo(session, authSession, user);
        var grantedRequired = scopeInfo.getGrantedRequired();
        var grantedOptional = scopeInfo.getGrantedOptional();
        var missingRequired = scopeInfo.getMissingRequired();
        var missingOptional = scopeInfo.getMissingOptional();
        var client = authSession.getClient();
        var realm = context.getRealm();

        // var scopeFieldMapping = getScopeFieldMapping(clientId);
        var requestedScopeNames = scopeInfo.getRequestedScopeNames();
        var scopeFieldMapping = ProfileClient.getProfileAttributesForConsentForm(session, realm, client, requestedScopeNames, user) //
                .getMapping();

        var fieldNameList = new ArrayList<String>();
        var scopes = new ArrayList<ScopeBean>();
        for (var currentScopes : List.of(grantedRequired, missingRequired, grantedOptional, missingOptional)) {
            for (var scope : currentScopes) {

                var fields = scopeFieldMapping.getOrDefault(scope.getName(), List.of()).stream().map(fun).collect(toList());
                fields.stream().map(ScopeFieldBean::getName).forEach(fieldNameList::add);
                var optional = currentScopes == grantedOptional || currentScopes == missingOptional;
                var granted = currentScopes == grantedRequired || currentScopes == grantedOptional;
                scopes.add(new ScopeBean(scope, optional, granted, fields));
            }
        }

        scopes.sort(ScopeBean.DEFAULT_ORDER);
        authSession.setAuthNote(CONSENTED_FIELD_LIST, String.join(",", fieldNameList));

        try {
            System.out.printf("Scope Profile Field Mapping: %s%n", JsonSerialization.writeValueAsString(scopes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        form.setAttribute("scopes", scopes);

        form.setAttribute("grantedScopeNames", scopeInfo.getGrantedScopeNames());

        form.setAttribute("grantedScope", String.join(" ", scopeInfo.getGrantedScopeNames()));
        form.setAttribute("requestedScope", String.join(" ", requestedScopeNames));

        if (formCustomizer != null) {
            formCustomizer.accept(form);
        }

        // use form from src/main/resources/theme-resources/templates/
        return form.createForm("select-consent-form.ftl");
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // handle consent selection from user
        var formParameters = context.getHttpRequest().getDecodedFormParameters();

        var authSession = context.getAuthenticationSession();
        var session = context.getSession();
        var users = context.getSession().users();
        var realm = context.getRealm();
        var event = context.getEvent();
        var client = authSession.getClient();
        var user = context.getUser();

        event.client(client).user(user).event(EventType.GRANT_CONSENT);

        var userId = user.getId();
        if (formParameters.getFirst("cancel") != null) {
            // User choose NOT to update consented scopes

            event.error(Errors.CONSENT_DENIED);
            // return to the application without consent update
            UserConsentModel consentModel = session.users().getConsentByClient(realm, userId, client.getId());
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
            var currentGrantedScopeNames = currentGrantedScopes.stream().map(ClientScopeModel::getName).collect(Collectors.toSet());
            var currentGrantedScopeNamesAsScope = String.join(",", currentGrantedScopeNames);
            context.getAuthenticationSession().setClientScopes(currentGrantedScopesIds);
            context.getAuthenticationSession().setClientNote(OAuth2Constants.SCOPE, "openid " + currentGrantedScopeNamesAsScope);

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
            var consentByClient = users.getConsentByClient(realm, userId, client.getId());
            var consentedFieldList = List.of(authSession.getAuthNote(CONSENTED_FIELD_LIST).split(","));

            var profileUpdate = new HashMap<String, String>();
            for (var fieldName : consentedFieldList) {
                profileUpdate.put(fieldName, formParameters.getFirst(fieldName));
            }

            var askedScopeNames = scopesToAskForConsent.stream().map(ClientScopeModel::getName).collect(Collectors.toSet());
            var profileUpdateResult = ProfileClient.updateProfileAttributesFromConsentForm(session, realm, client,
                    new LinkedHashSet<>(askedScopeNames), user, profileUpdate);
            // check profile result
            // if errors.isEmpty() -> proceed to context.success()
            // else show / populate form again with validation errors -> proceed with context.challenge(..)

            if (profileUpdateResult.hasErrors()) {

                context.challenge(createForm(context, form -> {
                    System.out.println("customize form with validation errors");
                    // form.setAttribute("")

                    List<FormMessage> fieldErrors = profileUpdateResult.getErrors().stream() //
                            .map(attributeError -> new FormMessage(attributeError.getAttributeName(), attributeError.getMessage())).collect(toList());
                    form.setErrors(fieldErrors);
                }));
                return;
            }

            if (consentByClient != null) {
                users.revokeConsentForClient(realm, userId, client.getId());
            }
            consentByClient = new UserConsentModel(client);

            scopesToAskForConsent.forEach(consentByClient::addGrantedClientScope);

            users.addConsent(realm, userId, consentByClient);

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

        private final List<ProfileAttribute> fields;
    }
}
