#!/usr/bin/env bash

lint_rego() {
    docker run --rm -v $PWD/iam:/opa/iam:z openpolicyagent/opa:0.44.0-envoy-static check --bundle /opa/iam
}

update_opa() {
    curl -s -o /dev/null -X PUT --data-binary @iam/keycloak/policy.rego  localhost:18181/v1/policies/iam/keycloak
}

publish_rego() {
  lint_rego && update_opa && echo "$(date +"%m-%d-%Y %T") OPA updated."
}

echo "Watching for changes in OPA policy files"
inotifywait --monitor --event close_write --recursive $PWD/iam | while read
do
  publish_rego
done

