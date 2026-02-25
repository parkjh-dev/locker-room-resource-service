package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.dto.request.*;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.security.CurrentUserId;
import com.lockerroom.resourceservice.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminUserListResponse>>> getUsers(
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUsers(pageRequest)));
    }

    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable Long userId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody SuspendRequest request) {
        adminService.suspendUser(userId, adminId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<CursorPageResponse<ReportListResponse>>> getReports(
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getReports(pageRequest)));
    }

    @PutMapping("/reports/{reportId}")
    public ResponseEntity<ApiResponse<Void>> processReport(
            @PathVariable Long reportId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody ReportProcessRequest request) {
        adminService.processReport(reportId, adminId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/notices")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> createNotice(
            @CurrentUserId Long adminId,
            @Valid @RequestBody NoticeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminService.createNotice(adminId, request)));
    }

    @PutMapping("/notices/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> updateNotice(
            @PathVariable Long noticeId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody NoticeCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateNotice(noticeId, adminId, request)));
    }

    @DeleteMapping("/notices/{noticeId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(
            @PathVariable Long noticeId) {
        adminService.deleteNotice(noticeId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/inquiries")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminInquiryListResponse>>> getInquiries(
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getInquiries(pageRequest)));
    }

    @PostMapping("/inquiries/{inquiryId}/reply")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> replyInquiry(
            @PathVariable Long inquiryId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody InquiryReplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminService.replyInquiry(inquiryId, adminId, request)));
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminRequestListResponse>>> getRequests(
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getRequests(pageRequest)));
    }

    @PutMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<RequestDetailResponse>> processRequest(
            @PathVariable Long requestId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody RequestProcessRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.processRequest(requestId, adminId, request)));
    }
}
