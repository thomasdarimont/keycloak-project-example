package com.github.thomasdarimont.keycloak.custom.consent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.models.UserModel;

import java.util.function.Function;

@Data
@NoArgsConstructor
public class ProfileAttribute {

    private String name;

    private String claimName;

    private String type;

    private String value;

    private boolean required;

    private boolean readonly;

    public ProfileAttribute(String name, String claimName, String type, String value, boolean required, boolean readonly) {
        this.name = name;
        this.claimName = claimName;
        this.type = type;
        this.value = value;
        this.required = required;
        this.readonly = readonly;
    }
}
