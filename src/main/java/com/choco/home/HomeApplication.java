package com.choco.home;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.google.api.client.util.Value;

import jakarta.annotation.PostConstruct;

@SpringBootApplication(exclude = {
		  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
		})
@EnableScheduling
public class HomeApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(HomeApplication.class, args);
	}
	
}
