package iam.keycloak

import future.keywords.in

# Map required client roles to clientId
required_roles := {
    "app-minispa": "acme-user",
    "app-keycloak-website": "acme-developer"
}

default allow = {
    "allow": false,
    "message": "access-denied"
}

# Users of the acme-internal realm need the acme-user role to access
allow = result {

    isRealm("acme-internal")

    hasRequiredRoleForClient(input.resource.clientId)

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

hasRequiredRoleForClient(clientId) = result {

    # if no explicit required_role is configured just use one of the existing realm roles
    requiredRole := object.get(required_roles, clientId, input.subject.realmRoles[0])

    # check if user contains required role
    result = requiredRole in input.subject.realmRoles
}