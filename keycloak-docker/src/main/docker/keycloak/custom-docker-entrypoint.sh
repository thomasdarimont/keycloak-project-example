#!/bin/bash
set -eou pipefail

# Workaround for alpine base image differences
if [[ -z ${BIND:-} ]]; then
#    BIND=$(hostname --all-ip-addresses)
    export BIND=$(hostname -i)
fi

# Call original Keycloak docker-entrypoint.sh
exec /opt/jboss/tools/docker-entrypoint.sh