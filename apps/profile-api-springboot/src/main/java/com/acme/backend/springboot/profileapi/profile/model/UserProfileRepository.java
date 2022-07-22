package com.acme.backend.springboot.profileapi.profile.model;

import org.springframework.stereotype.Repository;

@Repository
public class UserProfileRepository {

    public UserProfile getProfileByUserId(String userId) {

        UserProfile profile = new UserProfile();
        profile.setFirstName("Theo");
        profile.setLastName("Tester");
        profile.setTitle("Dr.");
        profile.setSalutation("Herr");
        profile.setBirthdate("");
        profile.setEmail("tester@local.de");
        profile.setPhoneNumber("+491234567");
        profile.setAddressStreet("Wanderweg 42");
        profile.setAddressCareOf("");
        profile.setAddressPostalCode("12345");
        profile.setAddressRegion("");
        profile.setAddressCountry("DE");

        return profile;
    }
}
