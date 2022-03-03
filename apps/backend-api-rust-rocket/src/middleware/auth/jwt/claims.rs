use rocket::serde::json::serde_json;
use std::collections::HashMap;

pub type Claims = HashMap<String, serde_json::Value>;
