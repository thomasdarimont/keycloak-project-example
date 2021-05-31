@echo off
echo "### Starting Environment with HTTPS"

docker-compose ^
  --env-file custom-keycloak-common.env ^
  -f docker-compose.yml ^
  -f docker-compose-tls.yml ^
  up --remove-orphans