package com.acme.backend.springboot.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendApiSpringbootApp {

	public static void main(String[] args) {
		SpringApplication.run(BackendApiSpringbootApp.class, args);
	}

}
