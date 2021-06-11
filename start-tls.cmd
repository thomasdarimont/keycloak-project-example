@echo off
echo "### Starting Environment with HTTPS"

docker-compose ^
  --env-file keycloak-common.env ^
  --file docker-compose.yml ^
  --file docker-compose-openldap.yml ^
  --file docker-compose-tls.yml ^
  up --remove-orphans