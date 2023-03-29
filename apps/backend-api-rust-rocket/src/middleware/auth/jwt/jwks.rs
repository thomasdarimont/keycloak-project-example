use crate::middleware::auth::jwt;
use crate::middleware::auth::jwt::get_max_age::get_max_age;
use crate::middleware::auth::jwt::JwtConfig;
use serde::Deserialize;
use std::error::Error;
use std::time::Duration;

#[derive(Debug, Deserialize)]
struct KeyResponse {
    keys: Vec<JwkKey>,
}

#[derive(Debug, Deserialize, Eq, PartialEq)]
pub struct JwkKey {
    pub e: String,
    pub alg: String,
    pub kty: String,
    pub kid: String,
    pub n: String,
}

#[derive(Debug, Deserialize, Eq, PartialEq)]
pub struct JwkKeys {
    pub keys: Vec<JwkKey>,
    pub validity: Duration,
}

// TODO make JWKS fetch FALLBACK_TIMEOUT configurable
const FALLBACK_TIMEOUT: Duration = Duration::from_secs(300);

pub fn fetch_keys_for_config(config: &JwtConfig) -> Result<JwkKeys, Box<dyn Error + Send>> {
    log::info!("Fetching JWKS Keys from URL={}", &config.jwk_url);
    let http_response = reqwest::blocking::get::<>(&config.jwk_url).unwrap();
    let max_age = get_max_age(&http_response).unwrap_or(FALLBACK_TIMEOUT);
    let result = Ok(http_response.json::<KeyResponse>().unwrap());

    result.map(|res| JwkKeys {
        keys: res.keys,
        validity: max_age,
    })
}

pub fn fetch_jwks_keys() -> Result<JwkKeys, Box<dyn Error + Send>> {
    return fetch_keys_for_config(&jwt::get_config());
}
