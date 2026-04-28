package com.lockerroom.resourceservice.inquiry.controller;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.infrastructure.aop.Idempotent;
import com.lockerroom.resourceservice.infrastructure.security.CurrentUserId;
import com.lockerroom.resourceservice.inquiry.dto.request.InquiryCreateRequest;
import com.lockerroom.resourceservice.inquiry.dto.response.InquiryDetailResponse;
import com.lockerroom.resourceservice.inquiry.dto.response.InquiryListResponse;
import com.lockerroom.resourceservice.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "문의(1:1)", description = "사용자 문의 등록/조회. 답변은 관리자가 등록하며 일반 사용자는 본인 문의만 조회 가능.")
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @Operation(summary = "문의 등록", description = "1:1 문의를 등록합니다. 첨부파일이 있으면 사전에 /api/v1/files로 업로드 후 ID 전달.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공")
    })
    @Idempotent
    @PostMapping
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> create(
            @CurrentUserId Long userId,
            @Valid @RequestBody InquiryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("문의가 등록되었습니다.", inquiryService.create(userId, request)));
    }

    @Operation(summary = "내 문의 목록", description = "본인이 등록한 문의를 커서 페이지네이션으로 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<InquiryListResponse>>> getMyList(
            @CurrentUserId Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getMyList(userId, pageRequest)));
    }

    @Operation(summary = "문의 상세 조회", description = "본인이 등록한 문의의 본문/첨부/답변을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "INQUIRY_ACCESS_DENIED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INQUIRY_NOT_FOUND")
    })
    @GetMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> getDetail(
            @PathVariable Long inquiryId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getDetail(inquiryId, userId)));
    }
}
