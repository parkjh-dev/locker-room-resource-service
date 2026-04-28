package com.lockerroom.resourceservice.sport.controller;

import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.sport.dto.response.SportResponse;
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

@Tag(name = "스포츠", description = "지원하는 스포츠 종목 조회 (익명 접근 가능)")
@RestController
@RequestMapping("/api/v1/sports")
@RequiredArgsConstructor
public class SportController {

    private final SportService sportService;

    @Operation(summary = "스포츠 종목 목록", description = "활성화된 스포츠 종목(축구, 야구 등) 목록을 반환합니다. 익명 접근 가능.")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<List<SportResponse>>> getSports() {
        return ResponseEntity.ok(ApiResponse.success(sportService.getSports()));
    }
}
