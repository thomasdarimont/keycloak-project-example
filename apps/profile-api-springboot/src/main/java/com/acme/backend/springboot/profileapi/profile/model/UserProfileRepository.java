package com.acme.backend.springboot.profileapi.profile.model;

import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Repository
public class UserProfileRepository {

    Map<String, UserProfile> profiles = new HashMap<>();

    @PostConstruct
    public void init() {
        profiles.put("",getTesterProfile());
    }

    public UserProfile getProfileByUserId(String userId) {
        return profiles.computeIfAbsent(userId, id -> {
            var profile = new UserProfile();
            profile.setId(userId);
            return profile;
        });
    }

    private UserProfile getTesterProfile() {
        var profile = new UserProfile();
        profile.setFirstName("Theodore");
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
