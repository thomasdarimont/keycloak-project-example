package com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient;

public interface AcmeAccountClient {

    AcmeUser getUserByUsername(String username);

    AcmeUser getUserByEmail(String email);

    AcmeUser getUserById(String userId);

    VerifyCredentialsOutput verifyCredentials(String userId, VerifyCredentialsInput input);

    UserSearchOutput searchForUsers(UserSearchInput userSearchInput);
}
