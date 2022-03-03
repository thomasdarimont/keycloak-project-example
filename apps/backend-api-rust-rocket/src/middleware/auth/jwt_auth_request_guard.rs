use crate::domain::user::User;
use crate::middleware::auth::jwt::JwtAuth;
use rocket::http::Status;
use rocket::outcome::Outcome;
use rocket::request;
use rocket::request::FromRequest;
use rocket::Request;
use rocket::State;

#[derive(Debug)]
pub enum AuthError {
    InvalidJwt,
    NoAuthorizationHeader,
    MultipleKeysProvided,
    NoJwkVerifier,
}

fn get_token_from_header(header: &str) -> Option<String> {
    let prefix_len = "Bearer ".len();

    match header.len() {
        l if l < prefix_len => None,
        _ => Some(header[prefix_len..].to_string()),
    }
}

fn verify_token(token: &String, auth: &JwtAuth) -> request::Outcome<User, AuthError> {
    let verified_token = auth.verify(&token);

    // TODO externalize claims to JWT User conversion
    let maybe_user = verified_token.map(|token| User {

        // TODO use more idiomatic value conversion here

        uid: token
            .claims
            .get("sub")
            .unwrap()
            .as_str()
            .unwrap()
            .to_string(),
        username: token
            .claims
            .get("preferred_username")
            .unwrap()
            .as_str()
            .unwrap()
            .to_string(),
        email: token
            .claims
            .get("email")
            .unwrap()
            .as_str()
            .unwrap()
            .to_string(),
    });
    match maybe_user {
        Some(user) => Outcome::Success(user),
        None => Outcome::Failure((Status::BadRequest, AuthError::InvalidJwt)),
    }
}

fn parse_and_verify_auth_header(header: &str, auth: &JwtAuth) -> request::Outcome<User, AuthError> {
    let maybe_token = get_token_from_header(header);

    match maybe_token {
        Some(token) => verify_token(&token, auth),
        None => Outcome::Failure((Status::Unauthorized, AuthError::InvalidJwt)),
    }
}

#[rocket::async_trait]
impl<'r> FromRequest<'r> for User {
    type Error = AuthError;

    async fn from_request(request: &'r Request<'_>) -> request::Outcome<Self, Self::Error> {
        let auth_headers: Vec<_> = request.headers().get("Authorization").collect();
        let configured_auth = request.guard::<&'r State<JwtAuth>>();

        match configured_auth.await {
            Outcome::Success(auth) => match auth_headers.len() {
                0 => Outcome::Failure((Status::Unauthorized, AuthError::NoAuthorizationHeader)),
                1 => parse_and_verify_auth_header(auth_headers[0], &auth),
                _ => Outcome::Failure((Status::BadRequest, AuthError::MultipleKeysProvided)),
            },
            _ => Outcome::Failure((Status::InternalServerError, AuthError::NoJwkVerifier)),
        }
    }
}

#[cfg(test)]
mod describe {
    #[test]
    fn test_extract_token() {
        let token = super::get_token_from_header("Bearer token_string");
        assert_eq!(Some("token_string".to_string()), token)
    }

    #[test]
    fn test_extract_token_too_short() {
        assert_eq!(None, super::get_token_from_header(&"Bear".to_string()));
        assert_eq!(None, super::get_token_from_header(&"Bearer".to_string()))
    }
}
