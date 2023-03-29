use std::env;

pub mod auth;
pub mod cors;
pub mod logging;

#[cfg(debug_assertions)]
pub fn expect_env_var(name: &str, default: &str) -> String {
    env::var(name).unwrap_or(String::from(default))
}

#[cfg(not(debug_assertions))]
pub fn expect_env_var(name: &str, _default: &str) -> String {
    return env::var(name).expect(&format!(
        "Environment variable {name} is not defined",
        name = name
    ));
}
