package com.acme.backend.springboot.profileapi.profile.schema;

import lombok.Data;

import java.util.List;

@Data
public class ScopedUserProfileAttributes {

    private String scope;

    private List<UserProfileAttribute> attributes;

    public ScopedUserProfileAttributes(String scope, UserProfileAttribute... attributes) {
        this.scope = scope;
        this.attributes = List.of(attributes);
    }
}