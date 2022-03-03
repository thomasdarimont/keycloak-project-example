# Backend API with JWK authentication based on Rocket (Rust)

## Features
- Validate JWT issued by Keycloak
- Validate JWT with JWK from JWKS endpoint
- Periodically fetch a JWKS Keyset from Keycloak
- Extract claims from JWT

## Run

```
ROCKET_PROFILE=debug cargo run
```

Browse to: https://127.0.0.1:4853


This example is inspired by [maylukas/rust_jwk_example](https://github.com/maylukas/rust_jwk_example)