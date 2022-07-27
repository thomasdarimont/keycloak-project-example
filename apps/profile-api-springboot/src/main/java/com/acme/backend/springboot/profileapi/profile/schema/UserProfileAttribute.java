package com.acme.backend.springboot.profileapi.profile.schema;

import com.acme.backend.springboot.profileapi.profile.model.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;


@Getter
@ToString
@AllArgsConstructor
public class UserProfileAttribute {

    // TODO discuss:
    // i18n: label, description

    // validation: clientSide, serverSide

    private String name;

    private String claimName;

    private String type;

    private String defaultValue;

    private Set<String> allowedValues;

    private boolean required;

    private boolean readonly;

    /**
     * Holds a function that defines the attribute value extraction logic.
     */
    private Function<UserProfile, String> accessor;

    private BiConsumer<UserProfile, String> mutator;

    public static Builder newAttribute() {
        return new Builder();
    }

    public String toClaimName() {
        if (this.claimName != null) {
            return claimName;
        }
        return this.name;
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
                .readonly(readonly) //
                .required(required) //
                .accessor(accessor) //
                .mutator(mutator);

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
        private boolean required;
        private boolean readonly;
        private Function<UserProfile, String> accessor;

        private BiConsumer<UserProfile, String> mutator;

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

        public Builder readonly(boolean readonly) {
            this.readonly = readonly;
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

        public UserProfileAttribute build() {
            return new UserProfileAttribute(name, claimName, type, defaultValue, allowedValues, required, readonly, accessor, mutator);
        }
    }
}
