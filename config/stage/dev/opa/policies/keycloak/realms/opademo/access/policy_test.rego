package keycloak.realms.opademo.access

#https://www.openpolicyagent.org/docs/latest/policy-testing/

import future.keywords.if
import future.keywords.in

import data.keycloak.utils.kc

test_access_account_console if {
    allow with input as {
                          "subject": {
                            "id": "c9d683de-4987-4e90-801e-81c6ac411d80",
                            "username": "tester",
                            "realmRoles": [
                              "default-roles-opademo",
                              "offline_access",
                              "uma_authorization",
                              "user"
                            ],
                            "clientRoles": [
                              "account:view-profile",
                              "account:manage-account",
                              "account:manage-account-links"
                            ],
                            "attributes": {
                              "emailVerified": true,
                              "email": "tester@local.de"
                            }
                          },
                          "resource": {
                            "realm": "opademo",
                            "clientId": "account-console"
                          },
                          "context": {
                            "attributes": {
                              "remoteAddress": "0:0:0:0:0:0:0:1"
                            }
                          },
                          "action": "access"
                        }

}

test_access_app1 if {
    allow with input as {
                          "subject": {
                            "id": "c9d683de-4987-4e90-801e-81c6ac411d80",
                            "username": "tester",
                            "realmRoles": [
                              "default-roles-opademo",
                              "offline_access",
                              "uma_authorization",
                              "user"
                            ],
                            "clientRoles": [
                              "account:view-profile",
                              "account:manage-account",
                              "account:manage-account-links",
                              "app1:access"
                            ],
                            "attributes": {
                              "emailVerified": true,
                              "email": "tester@local.de"
                            }
                          },
                          "resource": {
                            "realm": "opademo",
                            "clientId": "app1"
                          },
                          "context": {
                            "attributes": {
                              "remoteAddress": "0:0:0:0:0:0:0:1"
                            }
                          },
                          "action": "access"
                        }

}