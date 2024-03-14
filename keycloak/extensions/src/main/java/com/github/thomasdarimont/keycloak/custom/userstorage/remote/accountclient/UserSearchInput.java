package com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient;

import lombok.Data;

import java.util.EnumSet;

@Data
public class UserSearchInput {

    private final String search;

    private final Integer firstResult;

    private final Integer maxResults;

    private final EnumSet<UserSearchOptions> options;

    public UserSearchInput(String search, Integer firstResult, Integer maxResults, EnumSet<UserSearchOptions> options) {
        this.search = search;
        this.firstResult = firstResult;
        this.maxResults = maxResults;
        this.options = options;
    }

    public enum UserSearchOptions {
        COUNT_ONLY,INCLUDE_SERVICE_ACCOUNTS;
    }
}
