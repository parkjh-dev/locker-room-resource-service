package com.lockerroom.resourceservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InfoController {

	@Value("${spring.application.name}")
	private String applicationName;

	@GetMapping("/name")
	public String getName() {
		return applicationName;
	}

	@GetMapping("/version")
	public String getVersion() {
		return SpringBootVersion.getVersion();
	}
}
