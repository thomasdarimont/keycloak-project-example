use actix_4_jwt_auth::{OIDCValidator, OIDCValidatorConfig};
use actix_web::rt::task;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::BTreeMap as Map;

#[derive(Debug, Serialize, Deserialize)]
pub struct FoundClaims {
    pub iat: usize,
    pub exp: usize,
    pub iss: String,
    pub sub: String,
    pub scope: String,
    pub preferred_username: Option<String>,

    #[serde(flatten)]
    pub other: Map<String, Value>,
}

impl FoundClaims {
    pub fn has_scope(&self, scope: &str) -> bool {
        return self.scope
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
