package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.aop.Idempotent;
import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.request.InquiryCreateRequest;
import com.lockerroom.resourceservice.dto.response.ApiResponse;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.InquiryDetailResponse;
import com.lockerroom.resourceservice.dto.response.InquiryListResponse;
import com.lockerroom.resourceservice.security.CurrentUserId;
import com.lockerroom.resourceservice.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @Idempotent
    @PostMapping
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> create(
            @CurrentUserId Long userId,
            @Valid @RequestBody InquiryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inquiryService.create(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<InquiryListResponse>>> getMyList(
            @CurrentUserId Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getMyList(userId, pageRequest)));
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> getDetail(
            @PathVariable Long inquiryId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getDetail(inquiryId, userId)));
    }
}
