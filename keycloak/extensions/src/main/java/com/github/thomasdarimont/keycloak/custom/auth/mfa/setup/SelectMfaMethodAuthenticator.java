package com.github.thomasdarimont.keycloak.custom.auth.mfa.setup;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.credential.CredentialModel;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JBossLog
public class SelectMfaMethodAuthenticator implements Authenticator {

    private static final Set<String> DEFAULT_MFA_CREDENTIAL_TYPES = new LinkedHashSet<>(List.of(WebAuthnCredentialModel.TYPE_TWOFACTOR, OTPCredentialModel.TYPE));
    private static final Map<String, String> DEFAULT_MFA_CREDENTIAL_TYPE_TO_REQUIRED_ACTION_MAP = Map.ofEntries(Map.entry(WebAuthnCredentialModel.TYPE_TWOFACTOR, WebAuthnRegisterFactory.PROVIDER_ID), Map.entry(OTPCredentialModel.TYPE, UserModel.RequiredAction.CONFIGURE_TOTP.name()));

    private static final SelectMfaMethodAuthenticator INSTANCE = new SelectMfaMethodAuthenticator();
    public static final String MFA_CREDENTIAL_TYPES_KEY = "mfaCredentialTypes";

    public static final String MFA_CREDENTIAL_TYPES_REQUIRED_ACTION_MAP_KEY = "mfaCredentialTypesRequiredActionMap";

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        Set<String> mfaCredentialTypes = getConfiguredMfaCredentialTypes(context);

        // check if user has a MFA credential
        //context.getUser().credentialManager().getStoredCredentialsByTypeStream()
        if (isMfaCredentialConfiguredForCurrentUser(context.getUser(), mfaCredentialTypes)) {
            context.success();
            return;
        }

        // compute available mfa methods

        // generate form
        LoginFormsProvider form = context.form();
        form.setAttribute("mfaMethods", mfaCredentialTypes);
        Response selectMfaResponse = form.createForm("login-select-mfa-method.ftl");
        context.forceChallenge(selectMfaResponse);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // process mfa selection
        // issue proper required action to configure mfa
        MultivaluedMap<String, String> formParams = context.getHttpRequest().getDecodedFormParameters();

        // TODO handle invalid mfa method...
        String mfaMethod = formParams.getFirst("mfaMethod");

        Map<String, String> mapping = getConfiguredMfaCredentialTypesRequiredActionsMapping(context);
        String requiredActionId = mapping.get(mfaMethod);
        // TODO handle invalid providerid
        context.getAuthenticationSession().addRequiredAction(requiredActionId);

        context.success();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    boolean isMfaCredentialConfiguredForCurrentUser(UserModel user, Set<String> mfaCredentialTypes) {
        return user.credentialManager().getStoredCredentialsStream() //
                .map(CredentialModel::getType) //
                .anyMatch(mfaCredentialTypes::contains);
    }

    private static Set<String> getConfiguredMfaCredentialTypes(AuthenticationFlowContext context) {

        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        if (configModel == null) {
            return DEFAULT_MFA_CREDENTIAL_TYPES;
        }
        Map<String, String> config = configModel.getConfig();
        if (config == null) {
            return DEFAULT_MFA_CREDENTIAL_TYPES;
        }
        return Stream.of(config.get(MFA_CREDENTIAL_TYPES_KEY).split(",")).map(String::strip).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Map<String, String> getConfiguredMfaCredentialTypesRequiredActionsMapping(AuthenticationFlowContext context) {

        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        if (configModel == null) {
            return DEFAULT_MFA_CREDENTIAL_TYPE_TO_REQUIRED_ACTION_MAP;
        }
        Map<String, String> config = configModel.getConfig();
        if (config == null) {
            return DEFAULT_MFA_CREDENTIAL_TYPE_TO_REQUIRED_ACTION_MAP;
        }
        return stringToMap((String) config.get(MFA_CREDENTIAL_TYPES_REQUIRED_ACTION_MAP_KEY));
    }

    boolean isMfaCredential(CredentialModel cred) {
        switch (cred.getType()) {
            case WebAuthnCredentialModel.TYPE_TWOFACTOR:
                return true;
            case OTPCredentialModel.HOTP: // fall-through
            case OTPCredentialModel.TOTP:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOOP
    }

    @Override
    public void close() {

    }


    private static String mapToString(Map<String, String> map) {
        var list = new ArrayList<String>();
        for (var entry : map.entrySet()) {
            list.add(entry.getKey().trim() + ":" + entry.getValue().trim());
        }
        return String.join(",", list);
    }

    private static Map<String, String> stringToMap(String string) {
        var items = string.split(",");
        Map<String, String> map = new LinkedHashMap<>();
        for (var item : items) {
            String[] keyValue = item.split(":");
            map.put(keyValue[0], keyValue[1]);
        }
        return map;
    }

    @AutoService(AuthenticatorFactory.class)
    public static class Factory implements AuthenticatorFactory, ServerInfoAwareProviderFactory {

        @Override
        public String getId() {
            return "acme-auth-select-mfa";
        }

        @Override
        public Authenticator create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public String getDisplayType() {
            return "Acme: Select MFA Method";
        }

        @Override
        public String getHelpText() {
            return "Prompts the user to select an MFA Method";
        }

        @Override
        public String getReferenceCategory() {
            return "mfa";
        }

        @Override
        public boolean isConfigurable() {
            return true;
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {

            List<ProviderConfigProperty> properties = ProviderConfigurationBuilder.create() //
                    .property() //
                    .name(MFA_CREDENTIAL_TYPES_KEY) //
                    .label("MFA Credential Types") //
                    .helpText("Comma separated credential Types to treat as MFA credentials. Defaults to " + DEFAULT_MFA_CREDENTIAL_TYPES) //
                    .type(ProviderConfigProperty.STRING_TYPE) //
                    .defaultValue(String.join(",", DEFAULT_MFA_CREDENTIAL_TYPES)) //
                    .add()

                    .property() //
                    .name(MFA_CREDENTIAL_TYPES_REQUIRED_ACTION_MAP_KEY) //
                    .label("Required Action Mapping") //
                    .helpText("Comma separated mapping of MFA Credential Types to their Required Action. Format: credentialType:requiredActionProviderId. Defaults to " + mapToString(DEFAULT_MFA_CREDENTIAL_TYPE_TO_REQUIRED_ACTION_MAP)) //
                    .type(ProviderConfigProperty.STRING_TYPE) //
                    .defaultValue(mapToString(DEFAULT_MFA_CREDENTIAL_TYPE_TO_REQUIRED_ACTION_MAP)) //
                    .add()

                    .build();
            return properties;
        }


        @Override
        public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
            return REQUIREMENT_CHOICES;
        }

        @Override
        public boolean isUserSetupAllowed() {
            return false;
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            // called after factory is found
        }

        @Override
        public void init(Config.Scope config) {

            // spi-authenticator-acme-auth-hello-message
//            config.get("message");
            // called when provider factory is used
        }


        @Override
        public void close() {

        }

        @Override
        public Map<String, String> getOperationalInfo() {
            return Map.of("info", "infoValue");
        }
    }

}
