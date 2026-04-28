package com.lockerroom.resourceservice.infrastructure.controller;

import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영 정보", description = "서비스 메타정보(이름/버전) 조회. 헬스체크/CI/CD 용도. 익명 접근 가능.")
@RestController
@RequestMapping("/api/v1/info")
public class InfoController {

	@Value("${spring.application.name}")
	private String applicationName;

	@Operation(summary = "서비스 이름", description = "spring.application.name 설정값을 반환합니다.")
	@SecurityRequirements
	@GetMapping("/name")
	public ResponseEntity<ApiResponse<String>> getName() {
		return ResponseEntity.ok(ApiResponse.success("성공", applicationName));
	}

	@Operation(summary = "Spring Boot 버전", description = "런타임 Spring Boot 프레임워크 버전을 반환합니다.")
	@SecurityRequirements
	@GetMapping("/version")
	public ResponseEntity<ApiResponse<String>> getVersion() {
		return ResponseEntity.ok(ApiResponse.success("성공", SpringBootVersion.getVersion()));
	}
}
