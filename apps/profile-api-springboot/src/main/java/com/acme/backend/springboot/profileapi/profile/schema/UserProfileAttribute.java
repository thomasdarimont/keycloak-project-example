package com.acme.backend.springboot.profileapi.profile.schema;

import com.acme.backend.springboot.profileapi.profile.model.UserProfile;
import com.acme.backend.springboot.profileapi.profile.validation.UserProfileAttributeValidation;
import com.acme.backend.springboot.profileapi.profile.validation.UserProfileAttributeValidationErrors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;


@Getter
@ToString
@AllArgsConstructor
public class UserProfileAttribute {

    // TODO discuss: add support for dynamic field rendering via user-profile-commons.ftl in Keycloak
    // i18n: displayName, description, placeholder
    // grouping: group, groupDisplayHeader, groupDisplayDescription
    // autocomplete

    // validators: validators.options??
    //validators.options.options??
    //validators[attribute.annotations.inputOptionsFromValidation].options??

    private String name;

    private String claimName;

    private String type;

    private String defaultValue;

    private Set<String> allowedValues;

    private boolean required;

    private boolean readOnly;

    /**
     * <pre>
     * inputHelperTextBefore
     * inputHelperTextAfter
     * inputType?? (html5- ...)
     * inputTypePlaceholder??
     * inputTypePattern??
     * inputTypeSize??
     * inputTypeMaxlength??
     * inputTypeMinlength??
     * inputTypeMax??
     * inputTypeMin??
     * inputTypeStep??
     * inputTypeCols??
     * inputTypeRows??
     * inputTypeMaxlength??
     * inputTypeSize??
     * inputOptionsFromValidation??
     * inputOptionLabels??
     * inputOptionLabels[option]!option
     * inputOptionLabelsI18nPrefix??
     * </pre>
     */
    private Map<String, String> annotations;

    /**
     * Holds a function that defines the attribute value extraction logic.
     */
    private Function<UserProfile, String> accessor;

    /**
     * Holds a function that defines the attribute value mutation logic.
     */
    private BiConsumer<UserProfile, String> mutator;

    /**
     * Holds a function that defines the attribute value validation logic.
     */
    private UserProfileAttributeValidation validation;

    public static Builder newAttribute() {
        return new Builder();
    }

    public String toClaimName() {
        if (this.claimName != null) {
            return claimName;
        }
        return this.name;
    }

    public boolean isValid(UserProfile profile, String newValue, UserProfileAttributeValidationErrors errors) {

        if (validation != null) {
            return validation.test(profile, this, newValue, errors);
        }

        if (!CollectionUtils.isEmpty(allowedValues) && !allowedValues.contains(newValue)) {
            errors.addError("NOT_ALLOWED", this, "error-invalid-value");
            return false;
        }

        return !required || !ObjectUtils.isEmpty(newValue);
    }

    public void update(UserProfile profile, String newValue) {
        mutator.accept(profile, newValue);
    }

    /**
     * Returns a UserProfileAttributeBuilder that is configured with the copied values of this {@link UserProfileAttribute}.
     *
     * @return
     */
    public Builder customize() {
        var builder = newAttribute() //
                .name(name) //
                .claimName(claimName) //
                .type(type) //
                .defaultValue(defaultValue) //
                .readOnly(readOnly) //
                .required(required) //
                .annotations(annotations) //
                .accessor(accessor) //
                .mutator(mutator) //
                .validation(validation) //
                ;

        if (allowedValues != null) {
            builder.allowedValues(new LinkedHashSet<>(allowedValues));
        }

        return builder;
    }

    @ToString
    public static class Builder {
        private String name;
        private String claimName;
        private String type;
        private String defaultValue;
        private Set<String> allowedValues;
        private Map<String, String> annotations = Map.of();
        private boolean required;
        private boolean readOnly;
        private Function<UserProfile, String> accessor;

        private BiConsumer<UserProfile, String> mutator;

        private UserProfileAttributeValidation validation;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder claimName(String claimName) {
            this.claimName = claimName;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder allowedValues(Set<String> allowedValues) {
            this.allowedValues = allowedValues;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder annotations(Map<String, String> annotations) {
            this.annotations = annotations;
            return this;
        }

        public Builder accessor(Function<UserProfile, String> accessor) {
            this.accessor = accessor;
            return this;
        }

        public Builder mutator(BiConsumer<UserProfile, String> mutator) {
            this.mutator = mutator;
            return this;
        }

        public Builder validation(UserProfileAttributeValidation validation) {
            this.validation = validation;
            return this;
        }

        public UserProfileAttribute build() {
            return new UserProfileAttribute(name, claimName, type, defaultValue, allowedValues, required, readOnly, annotations, accessor, mutator, validation);
        }
    }
}
