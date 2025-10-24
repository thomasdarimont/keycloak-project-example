package iam.keycloak

import rego.v1

# Map required client roles to client_id
required_roles := {"app-minispa": "acme-user"}

#    "app-minispa": "acme-developer",
#    "app-keycloak-website": "acme-developer"

default allow := {
	"decision": false,
	"context": {
	    "message": "access-denied"
	}
}

# Users from acme-internal realm need the required roles to access
allow := result if {
	is_realm("acme-internal")

	has_required_role_for_client(input.resource.properties.clientId)

	result = _allow(true, "acme-user can access")
}

# Users from other realms can access
allow := result if {
	not is_realm("acme-internal")
	result = _allow(true, "every user can access")
}

# Helper function to return access decision with explanation
_allow(allow, hint) := result if {
	result = {
		"decision": allow,
			"context": {
        	    "message": hint
        	}
	}
}

is_realm(realm_name) := result if {
	result := input.resource.id == realm_name
}

has_required_role_for_client(client_id) := result if {
	# if no explicit required_role is configured just use one of the existing realm roles
	required_role := object.get(required_roles, client_id, input.subject.properties.realmRoles[0])

	# check if user contains required role
	result = required_role in input.subject.properties.realmRoles
}
