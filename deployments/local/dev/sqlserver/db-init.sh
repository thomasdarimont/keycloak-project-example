#!/usr/bin/env bash

#wait for the SQL Server to come up

echo "MSSQL: Waiting for MSSQL server to come up..."
while ! timeout 1 bash -c "echo > /dev/tcp/localhost/1433"; do
  sleep 1
done

sleep 3

echo "MSSQL: Create initial Keycloak database"
#run the setup script to create the DB and the schema in the DB
/opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -i /opt/mssql-tools/bin/db-init.sql

#/opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -Q "CREATE DATABASE keycloak" -C ;
#/opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -Q "CREATE USER keycloak WITH PASSWORD='Keycloak123'" -C ;