use crate::middleware::jwt_auth::FoundClaims;
use actix_4_jwt_auth::AuthenticatedUser;
use actix_web::{get, HttpResponse};
use chrono::Utc;
use serde::Serialize;

#[derive(Serialize)]
struct MeInfo {
    pub message: String,
    pub backend: String,
    pub datetime: String,
}

#[derive(Serialize)]
struct ErrorInfo {
    pub code: String,
}

#[get("/api/users/me")]
pub async fn handle_me_info(user: AuthenticatedUser<FoundClaims>) -> HttpResponse {
    if !user.claims.has_scope("email") {
        return HttpResponse::Forbidden().json(ErrorInfo {
            code: "invalid_scope".into(),
        });
    }

    let username = user.claims.preferred_username.unwrap_or("anonymous".into());
    let obj = MeInfo {
        message: format!("Hello, {}!", username),
        backend: "rust-actix".into(),
        datetime: Utc::now().to_string(),
    };
    HttpResponse::Ok().json(obj)
}
