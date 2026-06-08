package com.petsplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PetsPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetsPlatformApplication.class, args);
	}
}
