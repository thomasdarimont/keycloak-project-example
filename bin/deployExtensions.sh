#!/usr/bin/env bash

set -eou pipefail

docker-compose exec -T custom-keycloak \
  touch /opt/jboss/keycloak/standalone/deployments/keycloak-extensions.jar.dodeploy

echo "Deployment triggered"
