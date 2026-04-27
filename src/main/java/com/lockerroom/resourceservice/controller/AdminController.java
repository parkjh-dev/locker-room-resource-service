package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.dto.request.*;
import com.lockerroom.resourceservice.dto.response.*;
import com.lockerroom.resourceservice.model.enums.*;
import com.lockerroom.resourceservice.security.CurrentUserId;
import com.lockerroom.resourceservice.service.AdminService;
import com.lockerroom.resourceservice.service.NoticeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Validated
public class AdminController {

    private final AdminService adminService;
    private final NoticeService noticeService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboard()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminUserListResponse>>> getUsers(
            @ModelAttribute CursorPageRequest pageRequest,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) Role role) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUsers(pageRequest, keyword, role)));
    }

    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable Long userId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody SuspendRequest request) {
        adminService.suspendUser(userId, adminId, request);
        return ResponseEntity.ok(ApiResponse.success("사용자가 정지되었습니다."));
    }

    @PutMapping("/users/{userId}/unsuspend")
    public ResponseEntity<ApiResponse<Void>> unsuspendUser(
            @PathVariable Long userId,
            @CurrentUserId Long adminId) {
        adminService.unsuspendUser(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success("사용자 정지가 해제되었습니다."));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<CursorPageResponse<ReportListResponse>>> getReports(
            @ModelAttribute CursorPageRequest pageRequest,
            @RequestParam(required = false) ReportStatus status) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getReports(pageRequest, status)));
    }

    @PutMapping("/reports/{reportId}")
    public ResponseEntity<ApiResponse<Void>> processReport(
            @PathVariable Long reportId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody ReportProcessRequest request) {
        adminService.processReport(reportId, adminId, request);
        return ResponseEntity.ok(ApiResponse.success("신고가 처리되었습니다."));
    }

    @GetMapping("/notices")
    public ResponseEntity<ApiResponse<CursorPageResponse<NoticeListResponse>>> getNotices(
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getList(pageRequest)));
    }

    @PostMapping("/notices")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> createNotice(
            @CurrentUserId Long adminId,
            @Valid @RequestBody NoticeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("공지사항이 작성되었습니다.", adminService.createNotice(adminId, request)));
    }

    @PutMapping("/notices/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> updateNotice(
            @PathVariable Long noticeId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody NoticeCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("공지사항이 수정되었습니다.", adminService.updateNotice(noticeId, adminId, request)));
    }

    @DeleteMapping("/notices/{noticeId}")
    public ResponseEntity<Void> deleteNotice(
            @PathVariable Long noticeId) {
        adminService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inquiries")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminInquiryListResponse>>> getInquiries(
            @ModelAttribute CursorPageRequest pageRequest,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) InquiryType type) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getInquiries(pageRequest, status, type)));
    }

    @PostMapping("/inquiries/{inquiryId}/reply")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> replyInquiry(
            @PathVariable Long inquiryId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody InquiryReplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("문의 답변이 등록되었습니다.", adminService.replyInquiry(inquiryId, adminId, request)));
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminRequestListResponse>>> getRequests(
            @ModelAttribute CursorPageRequest pageRequest,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) RequestType type) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getRequests(pageRequest, status, type)));
    }

    @PutMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<RequestDetailResponse>> processRequest(
            @PathVariable Long requestId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody RequestProcessRequest request) {
        return ResponseEntity.ok(ApiResponse.success("요청이 처리되었습니다.", adminService.processRequest(requestId, adminId, request)));
    }
}
