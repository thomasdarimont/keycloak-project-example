use crate::middleware::auth::jwt::{fetch_jwks_keys, Claims, JwkKeys, JwtVerifier};
use crate::support::scheduling::use_repeating_job;
use jsonwebtoken::TokenData;
use std::sync::{Arc, Mutex};
use std::time::Duration;
use log;

type CleanupFn = Box<dyn Fn() -> () + Send>;

pub struct JwtAuth {
    verifier: Arc<Mutex<JwtVerifier>>,
    cleanup: Mutex<CleanupFn>,
}

impl Drop for JwtAuth {
    fn drop(&mut self) {
        // Stop the update thread when the updater is destructed
        let cleanup_fn = self.cleanup.lock().unwrap();
        cleanup_fn();
    }
}

impl JwtAuth {
    pub fn new() -> JwtAuth {
        let jwk_key_result = fetch_jwks_keys();
        let jwk_keys: JwkKeys = match jwk_key_result {
            Ok(keys) => keys,
            Err(_) => {
                panic!("Unable to fetch jwt keys! Cannot verify user tokens! Shutting down...")
            }
        };
        let verifier = Arc::new(Mutex::new(JwtVerifier::new(jwk_keys.keys)));

        let mut instance = JwtAuth {
            verifier: verifier,
            cleanup: Mutex::new(Box::new(|| {})),
        };

        instance.start_key_update();
        instance
    }

    pub fn verify(&self, token: &String) -> Option<TokenData<Claims>> {
        let verifier = self.verifier.lock().unwrap();
        verifier.verify(token)
    }

    fn start_key_update(&mut self) {
        let verifier_ref = Arc::clone(&self.verifier);

        let stop = use_repeating_job(move || match fetch_jwks_keys() {
            Ok(jwk_keys) => {
                let mut verifier = verifier_ref.lock().unwrap();
                verifier.set_keys(jwk_keys.keys);
                log::info!("Updated JWK keys. Next refresh will be in {:?}", jwk_keys.validity);
                jwk_keys.validity
            }
            Err(_) => Duration::from_secs(10),
        });

        let mut cleanup = self.cleanup.lock().unwrap();
        *cleanup = stop;
    }
}
