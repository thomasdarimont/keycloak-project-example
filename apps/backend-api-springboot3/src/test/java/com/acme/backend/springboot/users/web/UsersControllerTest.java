package com.acme.backend.springboot.users.web;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import com.acme.backend.springboot.users.config.WebSecurityConfig;
import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockJwtAuth;
import com.c4_soft.springaddons.security.oauth2.test.webmvc.jwt.AutoConfigureAddonsWebSecurity;

@WebMvcTest(controllers = UsersController.class)
@AutoConfigureAddonsWebSecurity
@Import(WebSecurityConfig.class)
class UsersControllerTest {

	@Autowired
	MockMvc api;

	@Test
	@WithAnonymousUser
	void givenRequestIsAnonymous_whenGetUsersMe_thenUnauthorized() throws Exception {
		// @formatter:off
		api.perform(get("/api/users/me").secure(true))
			.andExpect(status().isUnauthorized());
        // @formatter:on
	}

	@Test
	@WithMockJwtAuth(claims = @OpenIdClaims(sub = "Tonton Pirate"))
	void givenUserIsNotGrantedWithAccess_whenGetUsersMe_thenForbidden() throws Exception {
		// @formatter:off
        api.perform(get("/api/users/me").secure(true))
            .andExpect(status().isForbidden());
        // @formatter:on
	}

	@Test
	@WithMockJwtAuth(authorities = { "ROLE_ACCESS" }, claims = @OpenIdClaims(sub = "Tonton Pirate"))
	void givenUserIsGrantedWithAccess_whenGetUsersMe_thenOk() throws Exception {
		// @formatter:off
        api.perform(get("/api/users/me").secure(true))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", is("Hello Tonton Pirate")));
        // @formatter:on
	}

}
