package com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient;

import java.util.List;

public class UserSearchOutput {

    List<AcmeUser> users;

    int count;

    public List<AcmeUser> getUsers() {
        return users;
    }

    public int getCount() {
        return count;
    }
}
