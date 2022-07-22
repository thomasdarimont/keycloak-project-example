package com.acme.backend.springboot.profileapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProfileApiApp {

	public static void main(String[] args) {
		SpringApplication.run(ProfileApiApp.class, args);
	}

}
