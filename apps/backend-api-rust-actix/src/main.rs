#![feature(decl_macro)]

use actix_web::{App, HttpServer};
use actix_web::middleware::Logger;
use config::Config;

mod api;
mod config;
mod middleware;

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    let Config {
        server_bind_addr,
        cert_location,
        key_location,
        oidc_issuer,
        allowed_cors_origin,
        log_level_default,
    } = config::Config::from_environment_with_defaults();

    env_logger::init_from_env(env_logger::Env::new().default_filter_or(&log_level_default));

    let ssl_acceptor_builder = middleware::ssl::create_ssl_acceptor(&cert_location, &key_location);
    let oidc_jwt_validator = middleware::jwt_auth::create_oidc_jwt_validator(&oidc_issuer).await;

    HttpServer::new(move || {
        let cors = middleware::cors::create_cors_config(allowed_cors_origin.clone());

        App::new()
            .wrap(cors)
            // see https://actix.rs/actix-web/actix_web/middleware/struct.Logger.html
            .wrap(Logger::new("%a \"%r\" %s %b \"%{Referer}i\" %T"))
            .app_data(oidc_jwt_validator.clone())
            .service(api::me_info::handle_me_info)
    })
        .bind_openssl(server_bind_addr, ssl_acceptor_builder)?
        .run()
        .await
}
