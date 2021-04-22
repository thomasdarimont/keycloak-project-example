#!/usr/bin/env bash

set -eou pipefail

echo Trigger keycloak-extensions deployment.
touch /opt/jboss/keycloak/standalone/deployments/keycloak-extensions.jar.dodeploy
