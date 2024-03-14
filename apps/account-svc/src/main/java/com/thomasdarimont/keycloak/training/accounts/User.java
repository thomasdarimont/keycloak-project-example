package com.thomasdarimont.keycloak.training.accounts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class User implements Cloneable {

    private String id;
    private String username;
    private String email;
    private boolean emailVerified;
    private String firstname;
    private String lastname;
    private String password;
    private boolean enabled;
    private long created;
    private List<String> roles;

    public User(String id, String username, String password, String email, boolean emailVerified, String firstname, String lastname, boolean enabled, List<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.emailVerified = emailVerified;
        this.firstname = firstname;
        this.lastname = lastname;
        this.password = password;
        this.enabled = enabled;
        this.created = System.currentTimeMillis();
        this.roles = roles;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }
}