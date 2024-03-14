package com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AcmeUser implements Cloneable {

    private String id;
    private String username;
    private String email;
    private boolean emailVerified;
    private String firstname;
    private String lastname;
    private boolean enabled;
    private Long created;
    private List<String> roles;

    public AcmeUser(String id, String username, String email, boolean emailVerified, String firstname, String lastname, boolean enabled, List<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.emailVerified = emailVerified;
        this.firstname = firstname;
        this.lastname = lastname;
        this.enabled = enabled;
        this.created = System.currentTimeMillis();
        this.roles = roles;
    }
}