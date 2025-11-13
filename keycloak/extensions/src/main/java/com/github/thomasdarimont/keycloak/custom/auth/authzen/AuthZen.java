package com.github.thomasdarimont.keycloak.custom.auth.authzen;

import java.util.List;
import java.util.Map;

public class AuthZen {

    public record AccessRequest(Subject subject, Action action, Resource resource, Map<String, Object> context,
                                List<AccessEvaluation> evaluations, Map<String, Object> options) {
        public AccessRequest(Subject subject, Action action, Resource resource, Map<String, Object> context) {
            this(subject, action, resource, context, null, null);
        }
    }

    public record AccessEvaluation(Subject subject, Action action, Resource resource, Map<String, Object> context) {
        public AccessEvaluation(Resource resource) {
            this(null, null, resource, null);
        }

        public AccessEvaluation(Action action, Resource resource) {
            this(null, action, resource, null);
        }
    }

    /**
     * See: https://openid.github.io/authzen/#section-7.1.2.1
     */
    public enum EvaluationOption {
        execute_all,
        deny_on_first_deny,
        permit_on_first_permit,
    }

    public record Subject(String type, String id, Map<String, Object> properties) {
    }

    public record Action(String name, Map<String, Object> properties) {

        public Action(String name) {
            this(name, null);
        }
    }

    public record Resource(String type, Object id, Map<String, Object> properties) {
        public Resource(String type) {
            this(type, null, null);
        }
    }

    public record AccessResponse(Boolean decision, Map<String, Object> context) {
    }

    public record AccessEvaluationsResponse(List<AccessResponse> evaluations) {
    }

    public record SearchRequest(Subject subject, Action action, Resource resource, Map<String, Object> context, PageRequest page) {}

    public record PageRequest(String token, Integer limit, Map<String, Object> properties){
    }

    public record Page(String next_token, Integer count, Integer total, Map<String, Object> properties){
    }

    public record SearchResponse(Page page, Map<String, Object> context, List<Resource> results){
    }
}
