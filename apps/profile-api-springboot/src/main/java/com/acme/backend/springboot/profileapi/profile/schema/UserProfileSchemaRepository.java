package com.acme.backend.springboot.profileapi.profile.schema;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserProfileSchemaRepository {

    private final String DEFAULT_CLIENT = "default";

    public UserProfileSchema getProfileAttributes(String clientId) {

        var clientToScopeProfileAttributes = new LinkedHashMap<String, List<ScopedUserProfileAttributes>>();
        clientToScopeProfileAttributes.put(DEFAULT_CLIENT, List.of(

                new ScopedUserProfileAttributes("email", UserProfileAttribute.newAttribute().name("email").type("email").readonly(true).required(true).build()), //
                new ScopedUserProfileAttributes("phone", UserProfileAttribute.newAttribute().name("phoneNumber").type("tel").readonly(true).required(false).build()), //

                new ScopedUserProfileAttributes("birthdate", UserProfileAttribute.newAttribute().name("birthdate").type("text").readonly(true).required(false).build()), //

                new ScopedUserProfileAttributes("firstname", UserProfileAttribute.newAttribute().name("firstname").type("text").readonly(true).required(false).build()), //

                new ScopedUserProfileAttributes("name", //
                        UserProfileAttribute.newAttribute().name("salutation").type("text").readonly(false).required(false).build(), //
                        UserProfileAttribute.newAttribute().name("title").type("text").readonly(false).required(false).build(), //
                        UserProfileAttribute.newAttribute().name("firstName").type("text").readonly(false).required(true).build(), //
                        UserProfileAttribute.newAttribute().name("lastName").type("text").readonly(false).required(true).build() //
                ),

                new ScopedUserProfileAttributes("address", //
                        UserProfileAttribute.newAttribute().name("address.street").type("text").readonly(true).required(false).build(), //
                        UserProfileAttribute.newAttribute().name("address.careOf").type("text").readonly(true).required(false).build(), //
                        UserProfileAttribute.newAttribute().name("address.postalCode").type("text").readonly(true).required(false).build(), //
                        UserProfileAttribute.newAttribute().name("address.region").type("text").readonly(true).required(false).build(), //
                        UserProfileAttribute.newAttribute().name("address.country").type("text").readonly(true).required(false).build() //
                )));

        var defaultScopeProfileAttributes = clientToScopeProfileAttributes.get("default");

        var map = new HashMap<String, List<UserProfileAttribute>>();

        // add default scope field mapping
        copyScopeAttributeMappings(defaultScopeProfileAttributes, map);

        // override default scope attribute mapping if necessary
        if (clientToScopeProfileAttributes.containsKey(clientId)) {
            overrideScopeAttributeMappings(clientId, clientToScopeProfileAttributes, map);
        }

        return new UserProfileSchema(Collections.unmodifiableMap(map));
    }

    private void overrideScopeAttributeMappings(String clientId, Map<String, List<ScopedUserProfileAttributes>> clientToScopeProfileAttributes, Map<String, List<UserProfileAttribute>> map) {

        var clientScopeAttributeMappings = clientToScopeProfileAttributes.get(clientId);

        if (clientScopeAttributeMappings.isEmpty()) {
            return;
        }

        // remove default scope attribute for overridden scopes
        clientToScopeProfileAttributes.keySet().forEach(map::remove);

        // apply client specific scope mappings
        copyScopeAttributeMappings(clientScopeAttributeMappings, map);
    }

    private void copyScopeAttributeMappings(List<ScopedUserProfileAttributes> source, Map<String, List<UserProfileAttribute>> target) {
        for (var scopeAttributesMapping : source) {
            var attributes = target.computeIfAbsent(scopeAttributesMapping.getScope(), ignored -> new ArrayList<>());
            attributes.addAll(scopeAttributesMapping.getAttributes());
        }
    }
}
