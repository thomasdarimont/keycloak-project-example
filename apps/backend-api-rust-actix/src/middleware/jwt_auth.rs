use actix_4_jwt_auth::{OIDCValidator, OIDCValidatorConfig};
use actix_web::rt::task;
use serde_json::Value;
use std::collections::HashMap;

pub type FoundClaims = HashMap<String, Value>;

pub trait ClaimsAccessor {
    fn get_as_string(&self, key: &str) -> String;
    fn has_scope(&self, scope: &str) -> bool;
}

impl ClaimsAccessor for FoundClaims {
    fn get_as_string(&self, key: &str) -> String {
        return self.get(key).unwrap().as_str().unwrap().to_string();
    }

    fn has_scope(&self, scope: &str) -> bool {
        return self
            .get_as_string("scope")
            .split_ascii_whitespace()
            .into_iter()
            .any(|s| s == scope);
    }
}

pub async fn create_oidc_jwt_validator(issuer: &str) -> OIDCValidatorConfig {
    let iss = issuer.to_string();

    let config = task::spawn_blocking(move || {
        let validator = OIDCValidator::new_from_issuer(iss.clone()).unwrap();
        return OIDCValidatorConfig { issuer: iss, validator };
    })
    .await
    .unwrap();

    return config;
}
