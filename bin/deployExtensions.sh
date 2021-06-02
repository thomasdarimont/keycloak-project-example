#!/usr/bin/env bash

set -eou pipefail

docker-compose exec -T acme-keycloak \
  touch /opt/jboss/keycloak/standalone/deployments/extensions.jar.dodeploy

echo "Deployment triggered"
