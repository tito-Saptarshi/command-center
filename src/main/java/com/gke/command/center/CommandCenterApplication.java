package com.gke.command.center;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CommandCenterApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommandCenterApplication.class, args);
	}

}
