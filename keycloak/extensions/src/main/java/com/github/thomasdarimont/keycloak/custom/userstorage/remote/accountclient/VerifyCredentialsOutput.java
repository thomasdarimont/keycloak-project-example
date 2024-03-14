package com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VerifyCredentialsOutput {

    private boolean valid;

    public VerifyCredentialsOutput(boolean valid) {
        this.valid = valid;
    }
}