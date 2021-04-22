package com.github.thomasdarimont.keycloak.custom.support;

import java.util.List;

public class ScopeUtils {

    public static boolean hasScope(String requiredScope, String scopeParam) {

        if (scopeParam == null || scopeParam.isBlank()) {
            return false;
        }

        return List.of(scopeParam.split(" ")).contains(requiredScope);
    }
}
