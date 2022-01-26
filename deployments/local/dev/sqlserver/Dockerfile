ARG SQLSERVER_VERSION=2019-CU14-ubuntu-20.04
FROM mcr.microsoft.com/mssql/server:$SQLSERVER_VERSION

# Copy certificates into image to adjust permissions as necessary
# 10001 uid of sqlserver user
COPY --chown=10001:0 "./acme.test+1.pem" /var/opt/mssql/certs/mssql.pem
COPY --chown=10001:0 "./acme.test+1-key.pem" /var/opt/mssql/private/mssql.key
