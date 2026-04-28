package com.lockerroom.resourceservice.notice.controller;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.notice.dto.response.NoticeDetailResponse;
import com.lockerroom.resourceservice.notice.dto.response.NoticeListResponse;
import com.lockerroom.resourceservice.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "공지사항", description = "공지사항 목록/상세 조회 (모두 익명 접근 가능). 작성/수정/삭제는 관리자 API에서 처리.")
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지사항 목록", description = "공지사항을 커서 페이지네이션으로 반환합니다. 익명 접근 가능.")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<NoticeListResponse>>> getList(
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getList(pageRequest)));
    }

    @Operation(summary = "공지사항 상세 조회", description = "공지 본문/첨부를 조회합니다. 익명 접근 가능.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "NOTICE_NOT_FOUND")
    })
    @SecurityRequirements
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> getDetail(
            @PathVariable Long noticeId) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getDetail(noticeId)));
    }
}
