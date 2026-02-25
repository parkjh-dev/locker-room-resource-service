package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.aop.Idempotent;
import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.request.RequestCreateRequest;
import com.lockerroom.resourceservice.dto.response.ApiResponse;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.RequestDetailResponse;
import com.lockerroom.resourceservice.dto.response.RequestListResponse;
import com.lockerroom.resourceservice.security.CurrentUserId;
import com.lockerroom.resourceservice.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @Idempotent
    @PostMapping
    public ResponseEntity<ApiResponse<RequestDetailResponse>> create(
            @CurrentUserId Long userId,
            @Valid @RequestBody RequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(requestService.create(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<RequestListResponse>>> getMyList(
            @CurrentUserId Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(requestService.getMyList(userId, pageRequest)));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<RequestDetailResponse>> getDetail(
            @PathVariable Long requestId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success(requestService.getDetail(requestId, userId)));
    }
}
