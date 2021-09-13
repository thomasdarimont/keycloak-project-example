package com.github.thomasdarimont.keycloak.custom.auth.mfa.sms;

import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.client.SmsClientFactory;
import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.credentials.SmsCredentialModel;
import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.updatephone.UpdatePhoneNumberRequiredAction;
import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@AutoService(AuthenticatorFactory.class)
public class SmsAuthenticatorFactory implements AuthenticatorFactory, ServerInfoAwareProviderFactory {

    public static final int VERIFY_CODE_LENGTH = 6;

    public static final int CODE_TTL = 300;

    public static final SmsAuthenticator INSTANCE = new SmsAuthenticator();

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    static {
        List<ProviderConfigProperty> list = ProviderConfigurationBuilder
                .create()

                .property().name(SmsAuthenticator.CONFIG_CODE_LENGTH)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Code length")
                .defaultValue(VERIFY_CODE_LENGTH)
                .helpText("The length of the generated Code.")
                .add()

                .property().name(SmsAuthenticator.CONFIG_CODE_TTL)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Time-to-live")
                .defaultValue(CODE_TTL)
                .helpText("The time to live in seconds for the code to be valid.")
                .add()

                .property().name(SmsAuthenticator.CONFIG_MAX_ATTEMPTS)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Max Attempts")
                .defaultValue("5")
                .helpText("Max attempts for Code.")
                .add()

                .property().name(SmsAuthenticator.CONFIG_SENDER)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Sender")
                .defaultValue("$realmDisplayName")
                .helpText("Denotes the message sender of the SMS. Defaults to $realmDisplayName")
                .add()

                .property().name(SmsAuthenticator.CONFIG_CLIENT)
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(SmsClientFactory.MOCK_SMS_CLIENT)
                .label("Client")
                .defaultValue(SmsClientFactory.MOCK_SMS_CLIENT)
                .helpText("Denotes the client to send the SMS")
                .add()

                .property().name(SmsAuthenticator.CONFIG_PHONENUMBER_PATTERN)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Phone Number Pattern")
                .defaultValue("\\+49.*")
                .helpText("Regex Pattern for validation of Phone Numbers")
                .add()

                .property().name(SmsAuthenticator.CONFIG_USE_WEBOTP)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Use Web OTP")
                .defaultValue(true)
                .helpText("Appends the Web OTP fragment '@domain #code' after a newline to the sms message.")
                .add()

                .build();

        CONFIG_PROPERTIES = Collections.unmodifiableList(list);
    }

    @Override
    public String getId() {
        return "acme-sms-authenticator";
    }

    @Override
    public String getDisplayType() {
        return "Acme: SMS Authentication";
    }

    @Override
    public String getHelpText() {
        return "Validates a code sent via SMS.";
    }

    @Override
    public String getReferenceCategory() {
        return SmsCredentialModel.TYPE;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return INSTANCE;
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
    public Map<String, String> getOperationalInfo() {
        return Collections.singletonMap("availableClients", SmsClientFactory.getAvailableClientNames().toString());
    }

}