package com.github.thomasdarimont.keycloak.custom.registration.formaction;

import com.github.thomasdarimont.keycloak.custom.support.ScopeUtils;
import com.google.auto.service.AutoService;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.authentication.forms.RegistrationUserCreation;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.userprofile.AttributeGroupMetadata;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileMetadata;

import jakarta.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AutoService(FormActionFactory.class)
public class CustomRegistrationUserCreation extends RegistrationUserCreation {

    private static final String ID = "custom-registration-user-creation";

    private static final String TERMS_FIELD = "terms";
    private static final String TERMS_ACCEPTED_USER_ATTRIBUTE = "terms_accepted";
    private static final String ACCEPT_TERMS_REQUIRED_FORM_ATTRIBUTE = "acceptTermsRequired";
    private static final String TERMS_REQUIRED_MESSAGE = "termsRequired";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayType() {
        return "Custom Registration: User Creation with Terms";
    }

    @Override
    public void validate(ValidationContext context) {

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (!formData.containsKey(TERMS_FIELD)) {
            context.error(Errors.INVALID_REGISTRATION);
            formData.remove(TERMS_FIELD);

            List<FormMessage> errors = List.of(new FormMessage(TERMS_FIELD, TERMS_REQUIRED_MESSAGE));
            context.validationError(formData, errors);
            return;
        }

        // TODO validate dynamic custom profile fields

        super.validate(context);
    }

    @Override
    public void success(FormContext context) {
        super.success(context);
        context.getUser().setSingleAttribute(TERMS_ACCEPTED_USER_ATTRIBUTE, String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {

        form.setAttribute(ACCEPT_TERMS_REQUIRED_FORM_ATTRIBUTE, true);

//        addCustomDynamicProfileFields(context, form);
    }

    private void addCustomDynamicProfileFields(FormContext context, LoginFormsProvider form) {

        String scope = context.getAuthenticationSession().getClientNotes().get("scope");

        var upc = UserProfileContext.REGISTRATION_USER_CREATION;
        var profileMetadata = new UserProfileMetadata(upc);

        var groupAnnotations = Map.<String, Object>of();
        var additionalAttributeGroupMetadata = new AttributeGroupMetadata("additionalData", "Additional Data", "Additional Profile Data", groupAnnotations);

        int guiOrder = 10;
        if (ScopeUtils.hasScope(ScopeUtils.SCOPE_ACME_AGE_INFO, scope)) {
            var birthdateAttribute = profileMetadata.addAttribute(Constants.USER_ATTRIBUTES_PREFIX + "birthdate", guiOrder++) //
                    .setAttributeDisplayName("birthdate") //
                    .addAnnotations(Map.of("inputType", "html5-date", "required", Boolean.TRUE));
            birthdateAttribute.setAttributeGroupMetadata(additionalAttributeGroupMetadata);
        }

        if (ScopeUtils.hasScope(OAuth2Constants.SCOPE_PHONE, scope)) {
            var phoneNumberAttribute = profileMetadata.addAttribute(Constants.USER_ATTRIBUTES_PREFIX + "phone_number", guiOrder++) //
                    .setAttributeDisplayName("phoneNumber") //
                    .addAnnotations(Map.of("inputType", "html5-tel", "required", Boolean.FALSE));
            phoneNumberAttribute.setAttributeGroupMetadata(additionalAttributeGroupMetadata);
        }

        // TODO add more robust mechanism to support custom profile fields, see AcmeFreemarkerLoginFormsProvider
        form.setAttribute("customProfile", new CustomProfile(profileMetadata));
    }

    public static class CustomProfile {

        private final List<CustomAttribute> attributes;

        public CustomProfile(UserProfileMetadata profileMetadata) {
            this.attributes = createAttributes(profileMetadata);
        }

        private List<CustomAttribute> createAttributes(UserProfileMetadata profileMetadata) {
            if (profileMetadata.getAttributes() == null) {
                return List.of();
            }
            var attributes = new ArrayList<CustomAttribute>();
            for (var attr : profileMetadata.getAttributes()) {
                attributes.add(new CustomAttribute(attr, null));
            }
            return attributes;
        }

        public List<CustomAttribute> getAttributes() {
            return attributes;
        }
    }

    public static class CustomAttribute {

        private final AttributeMetadata attributeMetadata;

        private final AttributeGroupMetadata groupMetadata;

        private final String value;

        public CustomAttribute(AttributeMetadata attributeMetadata, String value) {
            this.attributeMetadata = attributeMetadata;
            this.groupMetadata = attributeMetadata.getAttributeGroupMetadata();
            this.value = value;
        }

        public String getName() {
            return this.attributeMetadata.getName();
        }

        public String getDisplayName() {
            return this.attributeMetadata.getAttributeDisplayName();
        }

        public boolean isRequired() {
            return Boolean.TRUE.equals(this.attributeMetadata.getAnnotations().get("required"));
        }

        public boolean isReadOnly() {
            return false;
        }

        public String getAutocomplete() {
            return null;
        }

        public String getValue() {
            return value;
        }

        public Map<String, Object> getAnnotations() {
            return attributeMetadata.getAnnotations();
        }

        public String getGroup() {
            return groupMetadata.getName();
        }

        public String getGroupDisplayHeader() {
            return groupMetadata.getDisplayHeader();
        }

        public String getGroupDisplayDescription() {
            return groupMetadata.getDisplayDescription();
        }

        public Map<String, Object> getGroupAnnotations() {
            return groupMetadata.getAnnotations();
        }
    }
}
