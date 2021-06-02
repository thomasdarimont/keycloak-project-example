#!/usr/bin/env bash

echo "### Starting Environment with HTTPS"

docker-compose \
  --env-file keycloak-common.env \
  -f docker-compose.yml \
  -f docker-compose-tls.yml \
  up --remove-orphans
