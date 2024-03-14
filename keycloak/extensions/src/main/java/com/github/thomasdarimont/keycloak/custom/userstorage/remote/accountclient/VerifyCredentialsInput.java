package com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VerifyCredentialsInput {
    private String password;

    public VerifyCredentialsInput(String password) {
        this.password = password;
    }
}

