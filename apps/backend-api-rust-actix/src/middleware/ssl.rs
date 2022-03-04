use openssl::ssl::{SslAcceptor, SslAcceptorBuilder, SslFiletype, SslMethod};

pub fn create_ssl_acceptor(cert_location: &str, key_location: &str) -> SslAcceptorBuilder {
    let mut builder = SslAcceptor::mozilla_intermediate(SslMethod::tls()).unwrap();
    builder
        .set_private_key_file(key_location, SslFiletype::PEM)
        .unwrap();
    builder.set_certificate_chain_file(cert_location).unwrap();

    return builder;
}
