package iam.keycloak

import future.keywords.in

default allow = {
    "allow": false,
    "message": "access-denied"
}

# Users of the acme-internal realm need the acme-user role to access
allow = result {

    input.resource.realm == "acme-internal"
    "acme-user" in input.subject.realmRoles

    result = _allow(true, "acme-user can access")
}

# Users of other realms can access
allow = result {

    input.resource.realm != "acme-internal"

    result = _allow(true, "every user can access")
}

# Helper function to return access decision with explanation
_allow(allow, hint) = result {
    result = {
        "allow": allow,
        "message": hint
    }
}