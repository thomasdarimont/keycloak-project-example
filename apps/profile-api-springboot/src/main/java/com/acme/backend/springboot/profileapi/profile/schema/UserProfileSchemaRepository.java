package com.acme.backend.springboot.profileapi.profile.schema;

import com.acme.backend.springboot.profileapi.profile.schema.UserProfileSchema.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.acme.backend.springboot.profileapi.profile.schema.UserProfileSchema.AddressAttributes;
import static com.acme.backend.springboot.profileapi.profile.schema.UserProfileSchema.PersonAttributes;

@Component
public class UserProfileSchemaRepository {

    public static final String DEFAULT_ATTRIBUTES_KEY = "default";

    public UserProfileSchema getProfileSchema(String clientId) {

        var clientToUserProfileAttributes = getUserProfileAttributeMapping();

        var map = new HashMap<String, List<UserProfileAttribute>>();

        // add default scope field mapping
        copyScopeAttributeMappings(clientToUserProfileAttributes.get(DEFAULT_ATTRIBUTES_KEY), map);

        // override default scope attribute mapping if necessary
        if (clientToUserProfileAttributes.containsKey(clientId)) {
            overrideScopeAttributeMappings(clientId, clientToUserProfileAttributes, map);
        }

        return new UserProfileSchema(Collections.unmodifiableMap(map));
    }

    private Map<String, List<ScopedUserProfileAttributes>> getUserProfileAttributeMapping() {
        var map = new HashMap<String, List<ScopedUserProfileAttributes>>();

        map.put(DEFAULT_ATTRIBUTES_KEY, List.of(
                new ScopedUserProfileAttributes(Scope.EMAIL, PersonAttributes.EMAIL), //
                new ScopedUserProfileAttributes(Scope.PHONE, PersonAttributes.PHONE_NUMBER), //
                new ScopedUserProfileAttributes(Scope.BIRTHDATE, PersonAttributes.BIRTHDATE), //
                new ScopedUserProfileAttributes(Scope.FIRSTNAME, PersonAttributes.FIRSTNAME), //

                new ScopedUserProfileAttributes(Scope.NAME, //
                        PersonAttributes.SALUTATION, //
                        PersonAttributes.TITLE, //
                        PersonAttributes.FIRSTNAME, //
                        PersonAttributes.LASTNAME //
                ),

                new ScopedUserProfileAttributes(Scope.ADDRESS, //
                        AddressAttributes.STREET, //
                        AddressAttributes.CARE_OF, //
                        AddressAttributes.POSTAL_CODE, //
                        AddressAttributes.REGION, //
                        AddressAttributes.COUNTRY //
                )));

        // TODO add client specific mappings here

        return map;
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
