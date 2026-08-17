package com.miniproject.verificationApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VerificationAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(VerificationAppApplication.class, args);
	}

}
