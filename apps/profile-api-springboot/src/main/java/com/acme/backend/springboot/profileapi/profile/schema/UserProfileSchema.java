package com.acme.backend.springboot.profileapi.profile.schema;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class UserProfileSchema {

    private final Map<String, List<UserProfileAttribute>> scopeAttributeMapping;

    public Map<String, List<UserProfileAttribute>> getScopeAttributeMapping(Set<String> scopes) {
        var result = new LinkedHashMap<>(scopeAttributeMapping);
        result.keySet().removeIf(key -> !scopes.contains(key));
        return result;
    }
}
