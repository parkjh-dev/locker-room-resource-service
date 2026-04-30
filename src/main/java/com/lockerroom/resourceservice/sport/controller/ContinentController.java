package com.lockerroom.resourceservice.sport.controller;

import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.sport.dto.response.ContinentResponse;
import com.lockerroom.resourceservice.sport.service.SportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "대륙", description = "대륙 메타데이터 (익명 접근 가능)")
@RestController
@RequestMapping("/api/v1/continents")
@RequiredArgsConstructor
public class ContinentController {

    private final SportService sportService;

    @Operation(summary = "대륙 목록", description = "전체 대륙 목록을 반환합니다. 익명 접근 가능.")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<List<ContinentResponse>>> getContinents() {
        return ResponseEntity.ok(ApiResponse.success(sportService.getContinents()));
    }
}
