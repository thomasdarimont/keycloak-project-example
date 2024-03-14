package com.thomasdarimont.keycloak.training.accounts;

import com.thomasdarimont.keycloak.training.accounts.UserResource.UserSearchInput;
import jakarta.inject.Singleton;
import lombok.extern.jbosslog.JBossLog;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Singleton
@JBossLog
class UserRepository {

    private static final List<User> USERS = new ArrayList<>();

    public UserRepository() {
        USERS.add(newUser("1", "bugs", "password", "bugs.bunny@acme.com", true, "Bugs", "Bunny", true, List.of("staff")));
        USERS.add(newUser("2", "daffy", "password", "duffy.duck@acme.com", true, "Duffy", "Duck", true, List.of("staff")));
        USERS.add(newUser("3", "porky", "password", "porky.pig@acme.com", false, "Porky", "Pig", false, List.of("staff")));
        USERS.add(newUser("4", "taz", "password", "taz.devil@acme.com", false, "Taz", "Devil", true, List.of("staff")));
        USERS.add(newUser("5", "sylvester", "password", "sylvester.cat@acme.com",false,  "Sylvester", "Cat", false, List.of("staff")));
        USERS.add(newUser("6", "marvin", "password", "marvin.martian@acme.com", false, "Marvin", "Martian", false, List.of("staff")));
        USERS.add(newUser("7", "wile", "password", "wile.e.coyote@acme.com", false, "Wile", "Coyote", false, null));
    }

    private static User newUser(String idSeed, String username, String password, String email, boolean emailVerified, String firstname, String lastname, boolean enabled, List<String> roles) {
        return new User(UUID.nameUUIDFromBytes(idSeed.getBytes()).toString(), username, password, email, emailVerified, firstname, lastname, enabled, roles);
    }

    public List<User> findAll() {
        log.info("findAll");
        return USERS;
    }

    public int count() {
        log.info("count");
        return USERS.size();
    }

    public User findById(String id) {
        log.infof("findById id=%s", id);
        return USERS.stream().filter(user -> user.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public User findByUsernameOrEmail(String username) {
        log.infof("findByUsernameOrEmail username=%s", username);
        return getByUsername(username).or(() -> getByEmail(username)).orElse(null);
    }

    public Optional<User> getByUsername(String username) {
        log.infof("getByUsername username=%s", username);
        return USERS.stream().filter(user -> user.getUsername().equalsIgnoreCase(username)).findFirst();
    }

    public Optional<User> getByEmail(String email) {
        log.infof("getByEmail email=%s", email);
        return USERS.stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst();
    }

    List<User> findUsers(String query) {
        log.infof("findUsers query=%s", query);
        return USERS.stream().filter(user -> query.equalsIgnoreCase("*") || user.getUsername().contains(query) || user.getEmail().contains(query)).toList();
    }

    public boolean validatePassword(String id, String password) {
        log.infof("validatePassword id=%s password=%s", id, password);
        return findById(id).getPassword().equals(password);
    }

    public boolean updatePassword(String id, String password) {
        log.infof("updatePassword id=%s password=%s", id, password);
        findById(id).setPassword(password);
        return true;
    }

    public void createUser(User user) {
        log.infof("createUser user=%s", user.toString());
        user.setId(UUID.randomUUID().toString());
        user.setCreated(System.currentTimeMillis());
        USERS.add(user);
    }

    public void updateUser(User user) {
        log.infof("updateUser user=%s", user.toString());
        User existing = findByUsernameOrEmail(user.getUsername());
        existing.setEmail(user.getEmail());
        existing.setFirstname(user.getFirstname());
        existing.setLastname(user.getLastname());
        existing.setEnabled(user.isEnabled());
    }

    public boolean removeUser(String id) {
        log.infof("removeUser id=%s", id);
        return USERS.removeIf(p -> p.getId().equals(id));
    }

    public List<User> search(String search, Integer firstResult, Integer maxResults, EnumSet<UserSearchInput.UserSearchOption> options) {
        log.infof("search search=%s firstResult=%s maxResults=%s options=%s", search, firstResult, maxResults, options);
        return searchInternal(search, firstResult, maxResults, options).toList();
    }

    public int searchForCount(String search, Integer firstResult, Integer maxResults, EnumSet<UserSearchInput.UserSearchOption> options) {
        log.infof("searchForCount search=%s firstResult=%s maxResults=%s options=%s", search, firstResult, maxResults, options);
        return (int) searchInternal(search, firstResult, maxResults, options).count();
    }

    private static Stream<User> searchInternal(String search, Integer firstResult, Integer maxResults, EnumSet<UserSearchInput.UserSearchOption> options) {

        if (search == null) {
            return Stream.empty();
        }

        var exact = search.startsWith("'") && search.endsWith("'");
        var exactSearch = exact ? search.substring(1, search.length() - 1) : search;

        final String searchMode;
        if (exact) {
            searchMode = "exact";
        } else if (search.trim().equals("*")) {
            searchMode = "wildcard";
        } else {
            searchMode = "contains";
        }

        log.infof("searchInternal search=%s firstResult=%s maxResults=%s options=%s searchMode=%s", search, firstResult, maxResults, options, searchMode);

        Stream<User> stream = USERS.stream() //
                .filter(u -> switch (searchMode) {
                    case "exact" -> u.getUsername().equals(exactSearch) || u.getEmail().equals(exactSearch);
                    case "contains" -> u.getUsername().contains(search) || u.getEmail().contains(search);
                    default /* wildcard / null */ -> true;
                }) //
                .filter(user -> !user.getUsername().startsWith("service-account-") || options.contains(UserSearchInput.UserSearchOption.INCLUDE_SERVICE_ACCOUNTS)) //
                ;

        if (firstResult != null) {
            stream = stream.skip(firstResult);
        }

        if (maxResults != null) {
            stream = stream.limit(maxResults);
        }

        return stream;
    }
}
