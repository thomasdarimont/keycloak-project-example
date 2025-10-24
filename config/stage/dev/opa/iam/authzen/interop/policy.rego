package iam.authzen.interop

import rego.v1

default decision := {
	"decision": false,
	"context": {"record": []},
}

decision := result if {
	ids := [r.id | r := data.iam.authzen.interop.records[_]; can_access(r)]
	result := {
		"decision": count(ids) > 0,
		"context": {"record": ids},
	}
}

can_access(r) if {
	r.owner == input.subject.id
}
