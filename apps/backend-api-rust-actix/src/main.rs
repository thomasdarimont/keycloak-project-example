#![feature(proc_macro_hygiene, decl_macro)]

use actix_web::{App, HttpServer};
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
    } = config::Config::from_environment_with_defaults();

    let ssl_acceptor_builder = middleware::ssl::create_ssl_acceptor(&cert_location, &key_location);
    let oidc_jwt_validator = middleware::jwt_auth::create_oidc_jwt_validator(&oidc_issuer).await;

    HttpServer::new(move || {
        let cors = middleware::cors::create_cors_config(allowed_cors_origin.clone());

        App::new()
            .wrap(cors)
            .app_data(oidc_jwt_validator.clone())
            .service(api::me_info::handle_me_info)
    })
    .bind_openssl(server_bind_addr, ssl_acceptor_builder)?
    .run()
    .await
}
