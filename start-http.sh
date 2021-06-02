#!/usr/bin/env bash

echo "### Starting Environment with Plain HTTP"

docker-compose \
  --env-file keycloak-common.env \
  -f docker-compose.yml \
  up --remove-orphans
