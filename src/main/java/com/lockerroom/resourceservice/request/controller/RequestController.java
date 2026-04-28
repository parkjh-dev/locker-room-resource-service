package com.lockerroom.resourceservice.request.controller;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.infrastructure.aop.Idempotent;
import com.lockerroom.resourceservice.infrastructure.security.CurrentUserId;
import com.lockerroom.resourceservice.request.dto.request.RequestCreateRequest;
import com.lockerroom.resourceservice.request.dto.response.RequestDetailResponse;
import com.lockerroom.resourceservice.request.dto.response.RequestListResponse;
import com.lockerroom.resourceservice.request.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "요청", description = "신규 팀/리그 등록 등 사용자 요청 등록/조회. 처리는 관리자가 수행.")
@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @Operation(summary = "요청 등록", description = "신규 팀/리그 등록 같은 사용자 요청을 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공")
    })
    @Idempotent
    @PostMapping
    public ResponseEntity<ApiResponse<RequestDetailResponse>> create(
            @CurrentUserId Long userId,
            @Valid @RequestBody RequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("요청이 등록되었습니다.", requestService.create(userId, request)));
    }

    @Operation(summary = "내 요청 목록", description = "본인이 등록한 요청을 커서 페이지네이션으로 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<RequestListResponse>>> getMyList(
            @CurrentUserId Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(requestService.getMyList(userId, pageRequest)));
    }

    @Operation(summary = "요청 상세 조회", description = "본인이 등록한 요청의 상세 내역과 처리 결과를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "REQUEST_ACCESS_DENIED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "REQUEST_NOT_FOUND")
    })
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<RequestDetailResponse>> getDetail(
            @PathVariable Long requestId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success(requestService.getDetail(requestId, userId)));
    }
}
