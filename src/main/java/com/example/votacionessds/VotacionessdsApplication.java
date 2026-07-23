package com.example.votacionessds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class VotacionessdsApplication {

	public static void main(String[] args) {
		SpringApplication.run(VotacionessdsApplication.class, args);
		log.info("Start Application, Hello World :)");
	}

}
