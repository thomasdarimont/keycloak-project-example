package com.github.thomasdarimont.keycloak.custom.consent;

import lombok.Data;
import org.keycloak.models.UserModel;

import java.util.function.Function;

@Data
public class ScopeField {

    private final String name;

    private final String type;

    private final Function<UserModel, String> valueAccessor;

    private final boolean required;
}
