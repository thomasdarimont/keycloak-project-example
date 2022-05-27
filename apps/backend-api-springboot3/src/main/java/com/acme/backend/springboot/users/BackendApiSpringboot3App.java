package com.acme.backend.springboot.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendApiSpringboot3App {

	public static void main(String[] args) {
		SpringApplication.run(BackendApiSpringboot3App.class, args);
	}

}
