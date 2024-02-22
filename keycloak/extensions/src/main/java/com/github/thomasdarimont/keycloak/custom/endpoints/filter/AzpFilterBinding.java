package com.github.thomasdarimont.keycloak.custom.endpoints.filter;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface AzpFilterBinding {
}
