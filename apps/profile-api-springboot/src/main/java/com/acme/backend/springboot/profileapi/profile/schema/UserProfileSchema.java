package com.acme.backend.springboot.profileapi.profile.schema;

import com.acme.backend.springboot.profileapi.profile.model.UserProfile;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.acme.backend.springboot.profileapi.profile.schema.UserProfileAttribute.newAttribute;
import static com.acme.backend.springboot.profileapi.profile.validation.UserProfileAttributeValidations.NOT_EMPTY;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;

@Data
public class UserProfileSchema {

    private final Map<String, List<UserProfileAttribute>> scopeAttributeMapping;

    public Map<String, List<UserProfileAttribute>> getScopeAttributeMapping(Set<String> scopes) {
        var result = new LinkedHashMap<>(scopeAttributeMapping);
        // only return profile attributes required by given scopes
        result.keySet().removeIf(key -> !scopes.contains(key));
        return result;
    }

    public interface Scope {

        // Standard Scopes below
        String EMAIL = "email";
        String PHONE = "phone";
        String NAME = "name";
        String ADDRESS = "address";
        String BIRTHDATE = "birthdate";

        // Custom Scopes below
        String FIRSTNAME = "firstname";
    }

    public interface PersonAttributes {

        UserProfileAttribute SALUTATION = newAttribute() //
                .name("salutation") //
                .type("text") //
                .readOnly(false) //
                .required(false) //
                .accessor(UserProfile::getSalutation) //
                .mutator(UserProfile::setSalutation) //
                .allowedValues(new LinkedHashSet<>(List.of("mr", "ms", "divers", ""))) //
                .annotations(ofEntries(entry("inputType", "select"))) //
                .build();
        UserProfileAttribute TITLE = newAttribute() //
                .name("title") //
                .type("text") //
                .readOnly(false) //
                .required(false) //
                .allowedValues(new LinkedHashSet<>(List.of("Dr.", "Prof. Dr.", ""))) //
                .annotations(ofEntries(entry("inputType", "select"))) //
                .accessor(UserProfile::getTitle) //
                .mutator(UserProfile::setTitle) //
                .build();
        UserProfileAttribute FIRSTNAME = newAttribute() //
                .name("firstname") //
                .claimName("given_name") //
                .type("text") //
                .readOnly(false) //
                .required(true) //
                .accessor(UserProfile::getFirstName) //
                .mutator(UserProfile::setFirstName) //
                .validation(NOT_EMPTY)
                .build();
        UserProfileAttribute LASTNAME = newAttribute() //
                .name("lastName") //
                .claimName("family_name") //
                .type("text") //
                .readOnly(false) //
                .required(true) //
                .accessor(UserProfile::getLastName) //
                .mutator(UserProfile::setLastName) //
                .validation(NOT_EMPTY)
                .build();
        UserProfileAttribute EMAIL = newAttribute() //
                .name("email") //
                .type("email") //
                .readOnly(false) //
                .required(true) //
                .accessor(UserProfile::getEmail) //
                .mutator(UserProfile::setEmail) //
                .build();
        UserProfileAttribute PHONE_NUMBER = newAttribute() //
                .name("phoneNumber") //
                .claimName("phone_number").type("tel") //
                .readOnly(false) //
                .required(false) //
                .accessor(UserProfile::getPhoneNumber) //
                .mutator(UserProfile::setPhoneNumber) //
                .build();
        UserProfileAttribute BIRTHDATE = newAttribute() //
                .name("birthdate") //
                .type("text") //
                .readOnly(false) //
                .required(false) //
                .accessor(UserProfile::getBirthdate) //
                .mutator(UserProfile::setBirthdate) //
                .build();
    }

    public interface AddressAttributes {

        UserProfileAttribute STREET = newAttribute() //
                .name("address.street") //
                .claimName("address_street") //
                .type("text") //
                .readOnly(false) //
                .required(false) //
                .accessor(UserProfile::getAddressStreet) //
                .mutator(UserProfile::setAddressStreet) //
                .build();
        UserProfileAttribute CARE_OF = newAttribute() //
                .name("address.careOf") //
                .claimName("address_care_of") //
                .type("text") //
                .readOnly(false) //
                .required(false) //
                .accessor(UserProfile::getAddressCareOf) //
                .mutator(UserProfile::setAddressCareOf) //
                .build();
        UserProfileAttribute POSTAL_CODE = newAttribute() //
                .name("address.postalCode") //
                .claimName("address_postal_code") //
                .type("text") //
                .readOnly(false) //
                .required(false) //
                .accessor(UserProfile::getAddressPostalCode) //
                .mutator(UserProfile::setAddressPostalCode) //
                .build();
        UserProfileAttribute REGION = newAttribute() //
                .name("address.region") //
                .claimName("address_region") //
                .type("text") //
                .readOnly(false) //
                .required(false) //
                .accessor(UserProfile::getAddressRegion) //
                .mutator(UserProfile::setAddressRegion) //
                .build();
        UserProfileAttribute COUNTRY = newAttribute() //
                .name("address.country") //
                .claimName("address_country") //
                .type("text") //
                .readOnly(false) //
                .required(false) //
                .accessor(UserProfile::getAddressCountry) //
                .mutator(UserProfile::setAddressCountry) //
                .allowedValues(new LinkedHashSet<>(List.of("DE", "FR", "ES", "EN")))
                .build();
    }
}
