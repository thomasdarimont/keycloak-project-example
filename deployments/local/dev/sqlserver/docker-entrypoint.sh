#!/usr/bin/env bash

#start SQL Server, start the script to create/setup the DB
# see https://github.com/microsoft/mssql-docker/issues/2#issuecomment-547699532
bash /opt/mssql/bin/db-init.sh &

/opt/mssql/bin/sqlservr