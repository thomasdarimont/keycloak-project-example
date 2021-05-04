#!/usr/bin/env bash

set -eou pipefail

docker-compose restart custom-keycloak-provisioning

echo "Provisioning triggered"
