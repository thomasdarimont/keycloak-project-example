//#![feature(proc_macro_hygiene, decl_macro)]

#[macro_use]
extern crate rocket;

use rocket::routes;
use std::error::Error;

use crate::domain::user::User;
use crate::middleware::auth::jwt::JwtAuth;
use chrono::Utc;
use rocket::serde::json::Json;
use rocket::tokio::task::spawn_blocking;

use serde::Deserialize;
use serde::Serialize;
use crate::middleware::logging;

pub mod domain;
pub mod middleware;
pub mod support;

#[derive(Serialize, Deserialize)]
pub struct MeInfo {
    pub message: String,
    pub backend: String,
    pub datetime: String,
}

#[options("/api/users/me")]
fn options_me_info() {}

#[get("/api/users/me")]
fn get_me_info(user: User) -> Json<MeInfo> {
    log::info!("Handle user info request. username={}", &user.username);

    let info = MeInfo {
        datetime: Utc::now().to_string(),
        message: format!("Hello, {}!", user.username),
        backend: String::from("rust-rocket"),
    };

    Json(info)
}

#[rocket::main]
async fn main() -> Result<(), Box<dyn Error>> {

    logging::init_logging();

    let auth = spawn_blocking(JwtAuth::new).await?;

    let _ = rocket::build()
        .attach(middleware::cors::Cors)
        .mount("/", routes![get_me_info, options_me_info])
        .manage(auth)
        .launch()
        .await?;

    Ok(())
}
