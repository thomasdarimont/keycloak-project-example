package keycloak.realms.opademo.access

import future.keywords.if
import future.keywords.in

import data.keycloak.utils.kc

# default allow rule: deny all
default allow := false

# allow acess to client-id:account-console if realm-role:user
allow if {
	kc.isClient("account-console")
	kc.hasRealmRole("user")
}

# allow acess to client-id:app1 if client-role:access
allow if {
	kc.isClient("app1")
	kc.hasCurrentClientRole("access")
}

# allow acess to client-id:app2 if client-role:access
allow if {
	kc.isClient("app2")
	kc.hasClientRole("app2", "access")
}

# allow acess to client-id:app3 if member of group
allow if {
	kc.isClient("app3")
	kc.isGroupMember("mygroup")
}

# allow acess to "special clients" if member of group
allow if {
	is_special_client(input.resource.clientId)
	kc.isGroupMember("foobargroup")
}

is_special_client(clientId) if startswith(clientId, "foo-")
is_special_client(clientId) if startswith(clientId, "bar-")

# https://www.styra.com/blog/how-to-express-or-in-rego/
