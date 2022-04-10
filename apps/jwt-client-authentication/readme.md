Example for JWT Client Authentication with Keycloak
---

# Generate Public / Private Key Pair
```
openssl req \
  -x509 \
  -newkey rsa:4096 \
  -keyout client_key.pem \
  -out client_cert.pem \
  -days 365 \
  -nodes \
  -subj "/CN=acme-service-client-jwt-auth"
```