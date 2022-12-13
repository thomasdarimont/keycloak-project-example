package iam.keycloak

import future.keywords.in

# Map required client roles to clientId
client_access_map := {
    "app-mini-spa": "acme-user"
}

default allow = {
    "allow": false,
    "message": "access-denied"
}

# Users of the acme-internal realm need the acme-user role to access
allow = result {

    isRealm("acme-internal")

#    requiredRole == client_access_map[input.resource.clientId]
#    requiredRole in input.subject.realmRoles

    result = _allow(true, "acme-user can access")
}

# Users of other realms can access
allow = result {

    not isRealm("acme-internal")
    result = _allow(true, "every user can access")
}

# Helper function to return access decision with explanation
_allow(allow, hint) = result {
    result = {
        "allow": allow,
        "message": hint
    }
}

isRealm(realmName) = result {
    result := input.resource.realm == realmName
}