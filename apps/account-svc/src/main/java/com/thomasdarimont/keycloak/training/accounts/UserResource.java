package com.thomasdarimont.keycloak.training.accounts;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

import java.util.EnumSet;
import java.util.List;

@JBossLog
@Path("/api/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class UserResource {

    private final UserRepository users;

    @GET
    public List<User> userList() {
        return users.findAll();
    }

    @GET
    @Path("{userId}")
    public User userById(@PathParam("userId") String userId) {
        return users.findById(userId);
    }


    @POST
    @Path("/lookup/username")
    public User lookupUserByUsername(UserLookupInput search) {
        return users.getByUsername(search.getUsername()).orElse(null);
    }

    @POST
    @Path("/lookup/email")
    public User lookupUserByEmail(UserLookupInput search) {
        return users.getByEmail(search.getEmail()).orElse(null);
    }

    @POST
    @Path("/{userId}/credentials/verify")
    public VerifyCredentialsOutput verifyCredentials(@PathParam("userId") String userId, VerifyCredentialsInput input) {
        VerifyCredentialsOutput output = verify(userId, input);
        log.infof("verifyCredentials output=%s", output);
        return output;
    }

    @POST
    @Path("/search")
    public UserSearchOutput searchUsers(UserSearchInput search) {

        if (search.getOptions().contains(UserSearchInput.UserSearchOption.COUNT_ONLY)) {
            int count = users.searchForCount(search.getSearch(), search.getFirstResult(), search.getMaxResults(), search.getOptions());
            return new UserSearchOutput(null, count);
        }

        var output = users.search(search.getSearch(), search.getFirstResult(), search.getMaxResults(), search.getOptions());
        log.infof("searchUsers output=%s", output);
        return new UserSearchOutput(output, output.size());
    }

    private VerifyCredentialsOutput verify(String userId, VerifyCredentialsInput input) {
        return new VerifyCredentialsOutput(users.validatePassword(userId, input.getPassword()));
    }

    @Data
    public static class UserLookupInput {
        String username;

        String email;
    }

    @Data
    public static class UserSearchInput {

        private String search;
        private Integer firstResult;
        private Integer maxResults;

        private EnumSet<UserSearchOption> options;

        public enum UserSearchOption {
            COUNT_ONLY, //

            INCLUDE_SERVICE_ACCOUNTS, //
        }
    }

    @Data
    public static class UserSearchOutput {

        private final List<User> users;

        private final int count;
    }

    @Data
    public static class VerifyCredentialsInput {
        String password;
    }

    @Data
    public static class VerifyCredentialsOutput {
        private final boolean valid;
    }

}
