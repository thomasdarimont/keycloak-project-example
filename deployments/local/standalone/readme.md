Keycloak Standalone Example
---

# Make certificates

mkcert -install

Generate "external cert"
mkcert -cert-file ./config/certs/acme.test-cert.pem -key-file ./config/certs/acme.test-cert-key.pem "*.acme.test"

Generate internal cert
mkcert -cert-file ./config/certs/keycloak-cert.pem -key-file ./config/certs/keycloak-cert-key.pem "keycloak"