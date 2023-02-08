package com.acme.backend.springboot.users;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockJwtAuth;
import com.c4_soft.springaddons.security.oauth2.test.webmvc.jwt.AutoConfigureAddonsWebSecurity;

@SpringBootTest
@AutoConfigureAddonsWebSecurity
@AutoConfigureMockMvc
class BackendApiSpringboot3AppTests {

	@Autowired
	MockMvc api;

	@Test
	@WithAnonymousUser
	void givenRequestIsAnonymous_whengetUsersMe_thenUnauthorized() throws Exception {
		// @formatter:off
		api.perform(get("/api/users/me").secure(true))
			.andExpect(status().isUnauthorized());
        // @formatter:on
	}

	@Test
	@WithMockJwtAuth(claims = @OpenIdClaims(sub = "Tonton Pirate"))
	void givenUserIsNotGrantedWithAccess_whengetUsersMe_thenForbidden() throws Exception {
		// @formatter:off
        api.perform(get("/api/users/me").secure(true))
            .andExpect(status().isForbidden());
        // @formatter:on
	}

	@Test
	@WithMockJwtAuth(authorities = { "ROLE_ACCESS" }, claims = @OpenIdClaims(sub = "Tonton Pirate"))
	void givenUserIsGrantedWithAccess_whengetUsersMe_thenOk() throws Exception {
		// @formatter:off
        api.perform(get("/api/users/me").secure(true))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", is("Hello Tonton Pirate")));
        // @formatter:on
	}

}
