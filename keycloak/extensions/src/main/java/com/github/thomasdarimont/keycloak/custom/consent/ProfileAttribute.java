package com.github.thomasdarimont.keycloak.custom.consent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.models.UserModel;

import java.util.function.Function;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileAttribute {

    private String name;

    private String type;

    private String value;

    private boolean required;

    private boolean readonly;
}
