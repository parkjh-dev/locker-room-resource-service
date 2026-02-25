package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.response.ApiResponse;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.NoticeDetailResponse;
import com.lockerroom.resourceservice.dto.response.NoticeListResponse;
import com.lockerroom.resourceservice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<NoticeListResponse>>> getList(
            @RequestParam(required = false) Long teamId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getList(teamId, pageRequest)));
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> getDetail(
            @PathVariable Long noticeId) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getDetail(noticeId)));
    }
}
