#!/usr/bin/env bash

set -eou pipefail

docker-compose restart acme-keycloak-provisioning

echo "Provisioning triggered"
