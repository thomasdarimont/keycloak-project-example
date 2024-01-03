package com.github.thomasdarimont.keycloak.custom.profile.phonenumber;

import com.google.auto.service.AutoService;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.validate.AbstractStringValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.ValidatorFactory;

import java.util.ArrayList;
import java.util.List;

@AutoService(ValidatorFactory.class)
public class AcmePhoneValidator extends AbstractStringValidator implements ConfiguredProvider {

    public static final String ID = "acme-phone-validator";

    public static final String DEFAULT_ERROR_MESSAGE_KEY = "error-invalid-phone-number";

    public static final String ERROR_MESSAGE_PROPERTY = "error-message";

    public static final String ALLOWED_NUMBERS_PATTERN_PROPERTY = "pattern";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    static {

        var configProperties = new ArrayList<ProviderConfigProperty>();
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ERROR_MESSAGE_PROPERTY);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setLabel("Error message key");
        property.setHelpText("Key of the error message in i18n bundle");
        property.setDefaultValue(DEFAULT_ERROR_MESSAGE_KEY);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ALLOWED_NUMBERS_PATTERN_PROPERTY);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setLabel("Allowed Number Pattern");
        property.setHelpText("Pattern for allowed phone numbers");
        property.setDefaultValue("[+]?\\d+");
        configProperties.add(property);

        CONFIG_PROPERTIES = configProperties;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getHelpText() {
        return "Validates a Phone number";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    protected void doValidate(String input, String inputHint, ValidationContext context, ValidatorConfig config) {

        // Simple example validations, use libphonenumber for more sophisticated validations, see: https://github.com/google/libphonenumber/tree/master
        if (config.get(ALLOWED_NUMBERS_PATTERN_PROPERTY) instanceof String patternString) {
            if (!input.matches(patternString)) {
                context.addError(new ValidationError(ID, inputHint, config.getStringOrDefault(ERROR_MESSAGE_PROPERTY, DEFAULT_ERROR_MESSAGE_KEY)));
            }
        }
    }
}
