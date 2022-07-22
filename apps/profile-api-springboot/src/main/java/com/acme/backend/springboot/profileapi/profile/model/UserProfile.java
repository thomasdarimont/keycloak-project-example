package com.acme.backend.springboot.profileapi.profile.model;

import lombok.Data;

@Data
public class UserProfile {

    String id;

    String firstName;

    String lastName;

    String title;

    String salutation;

    String email;

    // TODO clarify: is actually maintained by Keycloak
    boolean emailVerified;

    String phoneNumber;

    // TODO this needs to be store in the model, but must not be exposed to the client
    boolean phoneNumberVerified;

    String birthdate;

    // TODO discuss datamodel

    // TODO move to dedicated types
    String addressStreet;

    String addressCareOf;

    String addressPostalCode;

    String addressRegion;

    String addressCountry;

}
