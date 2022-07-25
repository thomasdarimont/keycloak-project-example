package com.acme.backend.springboot.profileapi.profile.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@Builder(builderMethodName = "newAttribute")
public class UserProfileAttribute {

    // TODO discuss:
    // i18n: label, description

    // validation: clientSide, serverSide

    private final String name;

    private final String claimName;

    private final String type;

    private final String defaultValue;

    private final Set<String> allowedValues;

    private final boolean required;

    private final boolean readonly;

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
    public UserProfileAttribute.UserProfileAttributeBuilder customize() {
        return newAttribute() //
                .name(name) //
                .claimName(claimName) //
                .type(type) //
                .defaultValue(defaultValue) //
                .allowedValues(new LinkedHashSet<>(allowedValues)) //
                .readonly(readonly) //
                .required(required);
    }
}
