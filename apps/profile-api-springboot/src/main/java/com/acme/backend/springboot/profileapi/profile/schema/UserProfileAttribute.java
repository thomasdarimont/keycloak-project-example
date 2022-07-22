package com.acme.backend.springboot.profileapi.profile.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "newAttribute")
public class UserProfileAttribute {

    // TODO discuss:
    // i18n: label, description

    // validation: clientSide, serverSide

    private String name;

    private String type;

    private String defaultValue;

    private Set<String> allowedValues;

    private boolean required;

    private boolean readonly;
}
