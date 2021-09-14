FROM postgres:11.12

# 999 uid of postgresql
COPY --chown=999:0 "./acme.test+1.pem" /var/lib/postgresql/server.crt
COPY --chown=999:0 "./acme.test+1-key.pem" /var/lib/postgresql/server.key
