#![feature(proc_macro_hygiene, decl_macro)]

#[macro_use]
extern crate rocket;

use rocket::routes;
use std::error::Error;

use crate::domain::user::User;
use crate::middleware::auth::jwt::JwtAuth;
use chrono::Utc;
use rocket::serde::json::Json;

use serde::Deserialize;
use serde::Serialize;

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
fn options_me_info() -> () {}

#[get("/api/users/me")]
fn get_me_info(user: User) -> Json<MeInfo> {
    let info = MeInfo {
        datetime: Utc::now().to_string().clone(),
        message: format!("Hello, {}!", user.username.to_string()),
        backend: String::from("rust-rocket").clone(),
    };

    return Json(info);
}

#[rocket::main]
async fn main() -> Result<(), Box<dyn Error>> {
    rocket::build()
        .attach(middleware::cors::Cors)
        .mount("/", routes![get_me_info, options_me_info])
        .manage(JwtAuth::new())
        .launch()
        .await?;

    Ok(())
}
