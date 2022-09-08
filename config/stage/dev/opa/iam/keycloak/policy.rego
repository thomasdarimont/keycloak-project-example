package iam.keycloak

default allow = {
    "allow": false,
    "message": "access-denied"
}

allow = result {
    input.subject.realmRoles[_] == "acme-user"

    result = {
        "allow": true,
        "message": "acme-user can access"
    }
}