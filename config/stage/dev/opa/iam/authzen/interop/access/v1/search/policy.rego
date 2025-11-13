package iam.authzen.interop.access.v1.search

import rego.v1

default resource := {
    "results": []
}

resource := result if {
    result := {
        "results": [{"id":r.id, "type": "record"} | r := data.iam.authzen.interop.records[_]; can_access(r)]
    }
}

can_access(r) if {
	r.owner == input.subject.id
}
