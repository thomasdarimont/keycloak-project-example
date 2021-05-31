@echo off
echo "### Starting Environment with Plain HTTP"

docker-compose ^
  --env-file custom-keycloak-common.env ^
  -f docker-compose.yml ^
  up --remove-orphans
