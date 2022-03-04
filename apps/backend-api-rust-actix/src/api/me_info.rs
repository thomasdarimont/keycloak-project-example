use actix_4_jwt_auth::AuthenticatedUser;
use actix_web::{get, HttpResponse};
use chrono::Utc;
use serde::Serialize;
use crate::middleware::jwt_auth::{Accessor, FoundClaims};

#[derive(Serialize)]
pub struct MeInfo {
    pub message: String,
    pub backend: String,
    pub datetime: String,
}

#[get("/api/users/me")]
pub async fn handle_me_info(user: AuthenticatedUser<FoundClaims>) -> HttpResponse {
    let username = user.claims.get_as_string("preferred_username");
    let obj = MeInfo {
        message: format!("Hello, {}!", username),
        backend: "rust-actix".to_string(),
        datetime: Utc::now().to_string().clone(),
    };
    HttpResponse::Ok().json(obj)
}
