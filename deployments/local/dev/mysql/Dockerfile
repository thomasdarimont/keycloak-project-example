FROM mysql/mysql-server:8.0.21

# Copy certificates into image to adjust permissions as necessary
# 27 uid of mysql user
COPY --chown=27:0 "./acme.test+1.pem" /etc/certs/ca.crt
COPY --chown=27:0 "./acme.test+1.pem" /etc/certs/server.crt
COPY --chown=27:0 "./acme.test+1-key.pem" /etc/certs/server.key
