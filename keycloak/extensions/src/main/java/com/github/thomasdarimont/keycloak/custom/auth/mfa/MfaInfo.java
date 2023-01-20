package com.github.thomasdarimont.keycloak.custom.auth.mfa;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MfaInfo {

    private final String type;
    private final String label;
}
