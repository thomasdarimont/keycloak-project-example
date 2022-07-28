package com.github.thomasdarimont.keycloak.custom.consent;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
public class ProfileAttribute {

    private String name;

    private String claimName;

    private String type;

    private String value;

    private Set<String> allowedValues;

    private boolean required;

    private boolean readonly;

    private Map<String, String> annotations;
}
