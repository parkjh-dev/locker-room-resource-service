package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.dto.response.ApiResponse;
import com.lockerroom.resourceservice.dto.response.SportResponse;
import com.lockerroom.resourceservice.service.SportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sports")
@RequiredArgsConstructor
public class SportController {

    private final SportService sportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SportResponse>>> getSports() {
        return ResponseEntity.ok(ApiResponse.success(sportService.getSports()));
    }
}
