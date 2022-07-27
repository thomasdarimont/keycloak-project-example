package com.acme.backend.springboot.profileapi.profile.schema;

import com.acme.backend.springboot.profileapi.profile.model.UserProfile;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.acme.backend.springboot.profileapi.profile.schema.UserProfileAttribute.newAttribute;

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
                .readonly(false) //
                .required(false) //
                .accessor(UserProfile::getSalutation) //
                .mutator(UserProfile::setSalutation) //
                .build();
        UserProfileAttribute TITLE = newAttribute() //
                .name("title") //
                .type("text") //
                .readonly(false) //
                .required(false) //
                .accessor(UserProfile::getTitle) //
                .mutator(UserProfile::setTitle) //
                .build();
        UserProfileAttribute FIRSTNAME = newAttribute() //
                .name("firstname") //
                .claimName("given_name") //
                .type("text") //
                .readonly(false) //
                .required(true) //
                .accessor(UserProfile::getFirstName) //
                .mutator(UserProfile::setFirstName) //
                .build();
        UserProfileAttribute LASTNAME = newAttribute() //
                .name("lastName") //
                .claimName("family_name") //
                .type("text") //
                .readonly(false) //
                .required(true) //
                .accessor(UserProfile::getLastName) //
                .mutator(UserProfile::setLastName) //
                .build();
        UserProfileAttribute EMAIL = newAttribute() //
                .name("email") //
                .type("email") //
                .readonly(false) //
                .required(true) //
                .accessor(UserProfile::getEmail) //
                .mutator(UserProfile::setEmail) //
                .build();
        UserProfileAttribute PHONE_NUMBER = newAttribute() //
                .name("phoneNumber") //
                .claimName("phone_number").type("tel") //
                .readonly(false) //
                .required(false) //
                .accessor(UserProfile::getPhoneNumber) //
                .mutator(UserProfile::setPhoneNumber) //
                .build();
        UserProfileAttribute BIRTHDATE = newAttribute() //
                .name("birthdate") //
                .type("text") //
                .readonly(false) //
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
                .readonly(false) //
                .required(false) //
                .accessor(UserProfile::getAddressStreet) //
                .mutator(UserProfile::setAddressStreet) //
                .build();
        UserProfileAttribute CARE_OF = newAttribute() //
                .name("address.careOf") //
                .claimName("address_care_of") //
                .type("text") //
                .readonly(false) //
                .required(false) //
                .accessor(UserProfile::getAddressCareOf) //
                .mutator(UserProfile::setAddressCareOf) //
                .build();
        UserProfileAttribute POSTAL_CODE = newAttribute() //
                .name("address.postalCode") //
                .claimName("address_postal_code") //
                .type("text") //
                .readonly(false) //
                .required(false) //
                .accessor(UserProfile::getAddressPostalCode) //
                .mutator(UserProfile::setAddressPostalCode) //
                .build();
        UserProfileAttribute REGION = newAttribute() //
                .name("address.region") //
                .claimName("address_region") //
                .type("text") //
                .readonly(false) //
                .required(false) //
                .accessor(UserProfile::getAddressRegion) //
                .mutator(UserProfile::setAddressRegion) //
                .build();
        UserProfileAttribute COUNTRY = newAttribute() //
                .name("address.country") //
                .claimName("address_country") //
                .type("text") //
                .readonly(false) //
                .required(false) //
                .accessor(UserProfile::getAddressCountry) //
                .mutator(UserProfile::setAddressCountry) //
                .build();
    }
}
