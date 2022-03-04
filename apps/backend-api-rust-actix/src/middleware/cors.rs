use actix_cors::Cors;
use actix_web::http::header;

pub fn create_cors_config(allowed_origin: String) -> Cors {
    Cors::default()
        .allowed_origin_fn(move |header, _request| {
            return header.as_bytes().ends_with(allowed_origin.as_bytes());
        })
        .allowed_methods(vec!["GET", "POST"])
        .allowed_headers(vec![header::AUTHORIZATION, header::ACCEPT])
        .allowed_header(header::CONTENT_TYPE)
        .supports_credentials()
        .max_age(3600)
}