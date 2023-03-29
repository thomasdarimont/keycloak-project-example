use crate::middleware::auth::jwt;
use crate::middleware::auth::jwt::claims::Claims;
use crate::middleware::auth::jwt::{JwkKey, JwtConfig};
use jsonwebtoken::decode_header;
use jsonwebtoken::TokenData;
use jsonwebtoken::{decode, Algorithm, DecodingKey, Validation};
use std::collections::HashMap;
use std::str::FromStr;

enum VerificationError {
    InvalidSignature,
    UnknownKeyAlgorithm,
}

#[derive(Debug)]
pub struct JwtVerifier {
    keys: HashMap<String, JwkKey>,
    config: JwtConfig,
}

fn keys_to_map(keys: Vec<JwkKey>) -> HashMap<String, JwkKey> {
    let mut keys_as_map = HashMap::new();
    for key in keys {
        keys_as_map.insert(String::clone(&key.kid), key);
    }
    keys_as_map
}

impl JwtVerifier {
    pub fn new(keys: Vec<JwkKey>) -> JwtVerifier {
        JwtVerifier {
            keys: keys_to_map(keys),
            config: jwt::get_config(),
        }
    }

    pub fn verify(&self, token: &str) -> Option<TokenData<Claims>> {
        let token_kid = match decode_header(token).map(|header| header.kid) {
            Ok(Some(header)) => header,
            _ => return None,
        };

        let jwk_key = match self.get_key(token_kid) {
            Some(key) => key,
            _ => return None,
        };

        match self.decode_token_with_key(jwk_key, token) {
            Ok(token_data) => Some(token_data),
            _ => None,
        }
    }

    pub fn set_keys(&mut self, keys: Vec<JwkKey>) {
        self.keys = keys_to_map(keys);
    }

    fn get_key(&self, key_id: String) -> Option<&JwkKey> {
        self.keys.get(&key_id)
    }

    fn decode_token_with_key(
        &self,
        key: &JwkKey,
        token: &str,
    ) -> Result<TokenData<Claims>, VerificationError> {
        // TODO ensure that "none" algorithm cannot be used!
        let algorithm = match Algorithm::from_str(&key.alg) {
            Ok(alg) => alg,
            Err(_error) => return Err(VerificationError::UnknownKeyAlgorithm),
        };

        let mut validation = Validation::new(algorithm);
        // TODO make audience validation configurable (enable / disable)
        // TODO make allowed audience configurable
        // validation.set_audience(&[&self.middleware.audience]);

        // TODO adapt to support multiple issuers
        let mut issuers = std::collections::HashSet::new();
        issuers.insert(self.config.issuer.clone());
        validation.iss = Some(issuers);

        let key = DecodingKey::from_rsa_components(&key.n, &key.e).unwrap();
        decode::<Claims>(token, &key, &validation)
            .map_err(|_| VerificationError::InvalidSignature)
    }
}
