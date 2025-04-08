package com.spring2025.vietchefs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class VietchefsApplication {

	public static void main(String[] args) {
		SpringApplication.run(VietchefsApplication.class, args);
	}

}
