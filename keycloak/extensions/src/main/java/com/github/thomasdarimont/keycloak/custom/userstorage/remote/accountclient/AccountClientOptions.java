package com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountClientOptions {

    String url;

    int connectTimeoutMillis;

    int readTimeoutMillis;

    int writeTimeoutMillis;
}
