#!/usr/bin/env bash

set -eou pipefail

realmName=${1:-custom}
additionalOptions=${@:2}

echo "Exporting $realmName"

docker-compose exec --env-file ../custom-keycloak.env keycloak \
  /opt/jboss/keycloak/bin/standalone.sh -c standalone.xml \
  -Djboss.socket.binding.port-offset=10000 \
  -Dkeycloak.migration.action=export \
  -Dkeycloak.migration.file=/opt/jboss/imex/$realmName-realm.json \
  -Dkeycloak.migration.provider=singleFile \
  -Dkeycloak.migration.realmName=$realmName \
  $additionalOptions
