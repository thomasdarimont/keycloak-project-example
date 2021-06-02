#!/usr/bin/env bash

set -eou pipefail

echo Trigger Keycloak extensions deployment.
touch /opt/jboss/keycloak/standalone/deployments/extensions.jar.dodeploy
