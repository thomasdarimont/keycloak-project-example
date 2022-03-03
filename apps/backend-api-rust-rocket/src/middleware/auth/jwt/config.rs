use crate::middleware;

#[derive(Debug)]
pub struct JwtConfig {
    pub jwk_url: String,
    pub audience: String,
    // TODO ADD support for multiple issuers via HashSet
    pub issuer: String,
}

pub fn get_config() -> JwtConfig {
    JwtConfig {
        jwk_url: middleware::expect_env_var(
            "JWK_URL",
            "https://id.acme.test:8443/auth/realms/acme-internal/protocol/openid-connect/certs",
        ),
        audience: middleware::expect_env_var("JWK_AUDIENCE", "app-minispa"),
        issuer: middleware::expect_env_var(
            "JWK_ISSUER",
            "https://id.acme.test:8443/auth/realms/acme-internal",
        ),
    }
}
