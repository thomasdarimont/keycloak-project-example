#![feature(proc_macro_hygiene, decl_macro)]

use actix_web::{App, HttpServer};

mod api;
mod middleware;

#[actix_web::main]
async fn main() -> std::io::Result<()> {

    // TODO allow configuration from environment
    let server_bind_addr = "127.0.0.1:4863";
    let cert_location = "../../config/stage/dev/tls/acme.test+1.pem";
    let key_location = "../../config/stage/dev/tls/acme.test+1-key.pem";
    let oidc_issuer = "https://id.acme.test:8443/auth/realms/acme-internal";
    let allowed_cors_origin = "https://apps.acme.test:4443";

    let ssl_acceptor_builder = middleware::ssl::create_ssl_acceptor(cert_location, key_location);
    let oidc_jwt_validator = middleware::jwt_auth::create_oidc_jwt_validator(oidc_issuer).await;

    HttpServer::new(move || {
        let cors = middleware::cors::create_cors_config(allowed_cors_origin.to_string());

        App::new() //
            .wrap(cors)
            .app_data(oidc_jwt_validator.clone())
            .service(api::me_info::handle_me_info) //
    }, //
    )
        .bind_openssl(server_bind_addr, ssl_acceptor_builder)?
        .run()
        .await
}
