#!/usr/bin/env bash

KEYCLOAK_JGROUPS_KEYSTORE_PASSWORD=${KEYCLOAK_JGROUPS_KEYSTORE_PASSWORD:-changeme3}

keytool -genseckey \
        -keyalg AES \
        -keysize 256 \
        -alias jgroups \
        -keystore ispn/jgroups.p12 \
        -deststoretype pkcs12 \
        -storepass ${KEYCLOAK_JGROUPS_KEYSTORE_PASSWORD} \
        -keypass ${KEYCLOAK_JGROUPS_KEYSTORE_PASSWORD} \
        -noprompt


