@echo off
echo "### Starting Environment with Plain HTTP"

docker-compose ^
  --env-file keycloak-common.env ^
  --file docker-compose.yml ^
  --file docker-compose-openldap.yml ^
  up --remove-orphans
