package com.example.mygenerics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

//@SpringBootApplication
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class MygenericsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MygenericsApplication.class, args);
	}

}
