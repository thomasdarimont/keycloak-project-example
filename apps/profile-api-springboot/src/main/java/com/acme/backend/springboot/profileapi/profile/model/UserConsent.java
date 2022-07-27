package com.acme.backend.springboot.profileapi.profile.model;

import lombok.Data;

import java.util.Set;

@Data
public class UserConsent {

    private final String userId;

    private final String clientId;

    private final Set<String> scopes;

    //createdAt

    //modifiedAt

}
