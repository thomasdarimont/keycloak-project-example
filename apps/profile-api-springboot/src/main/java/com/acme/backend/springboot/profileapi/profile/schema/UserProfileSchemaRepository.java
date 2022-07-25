package com.acme.backend.springboot.profileapi.profile.schema;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.acme.backend.springboot.profileapi.profile.schema.UserProfileAttribute.newAttribute;

@Component
public class UserProfileSchemaRepository {

    private final String DEFAULT_CLIENT = "default";

    private Map<String, List<ScopedUserProfileAttributes>> clientToScopeProfileAttributes;

    @PostConstruct
    public void init() {
        clientToScopeProfileAttributes = new HashMap<>();

        clientToScopeProfileAttributes.put(DEFAULT_CLIENT, List.of(

                new ScopedUserProfileAttributes("email", PersonAttributes.EMAIL), //
                new ScopedUserProfileAttributes("phone", PersonAttributes.PHONE_NUMBER), //
                new ScopedUserProfileAttributes("birthdate", PersonAttributes.BIRTHDATE), //
                new ScopedUserProfileAttributes("firstname", PersonAttributes.FIRSTNAME), //
                new ScopedUserProfileAttributes("name", //
                        PersonAttributes.SALUTATION, //
                        PersonAttributes.TITLE, //
                        PersonAttributes.FIRSTNAME, //
                        PersonAttributes.LASTNAME //
                ),

                new ScopedUserProfileAttributes("address", //
                        AddressAttributes.STREET, //
                        AddressAttributes.CARE_OF, //
                        AddressAttributes.POSTAL_CODE, //
                        AddressAttributes.REGION, //
                        AddressAttributes.COUNTRY //
                )));
    }

    public UserProfileSchema getProfileAttributes(String clientId) {

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


    interface PersonAttributes {

        UserProfileAttribute SALUTATION = newAttribute().name("salutation").type("text").readonly(false).required(false).build();
        UserProfileAttribute TITLE = newAttribute().name("title").type("text").readonly(false).required(false).build();
        UserProfileAttribute FIRSTNAME = newAttribute().name("firstname").claimName("given_name").type("text").readonly(true).required(false).build();
        UserProfileAttribute LASTNAME = newAttribute().name("lastName").claimName("family_name").type("text").readonly(false).required(true).build();
        UserProfileAttribute EMAIL = newAttribute().name("email").type("email").readonly(true).required(true).build();
        UserProfileAttribute PHONE_NUMBER = newAttribute().name("phoneNumber").type("tel").readonly(true).required(false).build();
        UserProfileAttribute BIRTHDATE = newAttribute().name("birthdate").type("text").readonly(true).required(false).build();
    }

    interface AddressAttributes {

        UserProfileAttribute STREET = newAttribute().name("address.street").type("text").readonly(true).required(false).build();
        UserProfileAttribute CARE_OF = newAttribute().name("address.careOf").type("text").readonly(true).required(false).build();
        UserProfileAttribute POSTAL_CODE = newAttribute().name("address.postalCode").type("text").readonly(true).required(false).build();
        UserProfileAttribute REGION = newAttribute().name("address.region").type("text").readonly(true).required(false).build();
        UserProfileAttribute COUNTRY = newAttribute().name("address.country").type("text").readonly(true).required(false).build();
    }
}
