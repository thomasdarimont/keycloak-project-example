package com.github.thomasdarimont.keycloak.custom.ubersession;

import org.keycloak.TokenCategory;
import org.keycloak.representations.JsonWebToken;

public class UberSessionToken extends JsonWebToken {

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.INTERNAL;
    }
}
