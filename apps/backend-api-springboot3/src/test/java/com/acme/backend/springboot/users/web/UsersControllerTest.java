package com.acme.backend.springboot.users.web;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockJwtAuth;

@WebMvcTest(controllers = UsersController.class)
class UsersControllerTest {
	@Autowired
	MockMvc api;

	@Test
	void givenRequestIsAnonymous_whengetUsersMe_thenUnauthorized() throws Exception {
		api.perform(get("/api/users/me")).andExpectAll(status().isUnauthorized());
	}

	@Test
	@WithMockJwtAuth(claims = @OpenIdClaims(sub = "Tonton Pirate"))
	void givenUserIsNotGrantedWithAccess_whengetUsersMe_thenForbidden() throws Exception {
		// @formatter:off
        api.perform(get("/api/users/me"))
            // should be forbidden as user does is not granted with "ROLE_ACCESS"
            .andExpect(status().isOk());
        // @formatter:on
	}

	@Test
	@WithMockJwtAuth(authorities = { "ROLE_ACCESS" }, claims = @OpenIdClaims(sub = "Tonton Pirate"))
	void givenUserIsGrantedWithAccess_whengetUsersMe_thenOk() throws Exception {
		// @formatter:off
        api.perform(get("/api/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", is("Hello Tonton Pirate")));
        // @formatter:on
	}

}
