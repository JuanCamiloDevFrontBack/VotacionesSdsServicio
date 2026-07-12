package com.example.votacionessds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VotacionessdsApplication {

	public static void main(String[] args) {
		SpringApplication.run(VotacionessdsApplication.class, args);
		System.out.println("Hello World :)");
	}

}
