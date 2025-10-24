package com.github.thomasdarimont.keycloak.custom.auth.authzen;

import java.util.Map;

public class AuthZen {

    public record AccessRequest(Subject subject, Resource resource, Map<String, Object> context, Action action) {
    }

    public record Subject(String type, String id, Map<String, Object> properties) {
    }

    public record Resource(String type, String id, Map<String, Object> properties) {
        public Resource(String type) {
            this(type, null, null);
        }

        public Resource(String type, String id) {
            this(type, id, null);
        }
    }

    public record Action(String name, Map<String, Object> properties) {

        public Action(String name) {
            this(name, null);
        }
    }

    public record Decision(boolean decision, Map<String, Object> context) {}
}
