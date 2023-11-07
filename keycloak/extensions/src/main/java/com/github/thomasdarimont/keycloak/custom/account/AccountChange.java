package com.github.thomasdarimont.keycloak.custom.account;

import lombok.Data;

@Data
public class AccountChange {

    private final String changedAttribute;

    private final String changedValue;
}
