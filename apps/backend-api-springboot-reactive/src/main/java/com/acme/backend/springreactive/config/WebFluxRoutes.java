package com.acme.backend.springreactive.config;

import com.acme.backend.springreactive.users.UserHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration
class WebFluxRoutes {

    @Bean
    public RouterFunction<ServerResponse> route(UserHandlers userHandlers) {

        return RouterFunctions.route( //
                GET("/api/users/me").and(accept(MediaType.APPLICATION_JSON)), userHandlers::me) //
                ;
    }
}
