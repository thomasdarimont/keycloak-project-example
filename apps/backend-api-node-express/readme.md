Acme Backend API Node Express
---

# Setup

Add rootCA for self-signed certificates - required for fetching public keys from JWKS endpoint in Keycloak. 
```
export NODE_EXTRA_CA_CERTS=$(mkcert -CAROOT)/rootCA.pem
```

# Build

```
yarn install
```

# Run
```
yarn run start
```