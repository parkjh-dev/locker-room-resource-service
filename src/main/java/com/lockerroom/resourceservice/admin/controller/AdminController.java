package com.lockerroom.resourceservice.admin.controller;

import com.lockerroom.resourceservice.admin.dto.request.ReportProcessRequest;
import com.lockerroom.resourceservice.admin.dto.request.SuspendRequest;
import com.lockerroom.resourceservice.admin.dto.response.AdminDashboardResponse;
import com.lockerroom.resourceservice.admin.service.AdminService;
import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.common.model.enums.Role;
import com.lockerroom.resourceservice.infrastructure.security.CurrentUserId;
import com.lockerroom.resourceservice.inquiry.dto.request.InquiryReplyRequest;
import com.lockerroom.resourceservice.inquiry.dto.response.AdminInquiryListResponse;
import com.lockerroom.resourceservice.inquiry.dto.response.InquiryDetailResponse;
import com.lockerroom.resourceservice.inquiry.model.enums.InquiryStatus;
import com.lockerroom.resourceservice.inquiry.model.enums.InquiryType;
import com.lockerroom.resourceservice.notice.dto.request.NoticeCreateRequest;
import com.lockerroom.resourceservice.notice.dto.response.NoticeDetailResponse;
import com.lockerroom.resourceservice.notice.dto.response.NoticeListResponse;
import com.lockerroom.resourceservice.notice.service.NoticeService;
import com.lockerroom.resourceservice.post.dto.response.ReportListResponse;
import com.lockerroom.resourceservice.post.model.enums.ReportStatus;
import com.lockerroom.resourceservice.request.dto.request.RequestProcessRequest;
import com.lockerroom.resourceservice.request.dto.response.AdminRequestListResponse;
import com.lockerroom.resourceservice.request.dto.response.RequestDetailResponse;
import com.lockerroom.resourceservice.request.model.enums.RequestStatus;
import com.lockerroom.resourceservice.request.model.enums.RequestType;
import com.lockerroom.resourceservice.user.dto.response.AdminUserListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자", description = "관리자 전용 API. ROLE_ADMIN 권한 필요. 사용자/신고/공지/문의/요청 통합 관리.")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Validated
public class AdminController {

    private final AdminService adminService;
    private final NoticeService noticeService;

    @Operation(summary = "[관리자] 대시보드 통계", description = "신고/문의/요청 등 미처리 항목 카운트와 가입자/게시글 통계를 반환합니다.")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboard()));
    }

    @Operation(summary = "[관리자] 사용자 목록 조회", description = "닉네임/이메일 키워드 및 권한(role) 필터링 가능. 커서 페이지네이션.")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminUserListResponse>>> getUsers(
            @ModelAttribute CursorPageRequest pageRequest,
            @Parameter(description = "닉네임/이메일 검색어 (최대 100자)") @RequestParam(required = false) @Size(max = 100) String keyword,
            @Parameter(description = "권한 필터 (USER, ADMIN)") @RequestParam(required = false) Role role) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUsers(pageRequest, keyword, role)));
    }

    @Operation(summary = "[관리자] 사용자 정지", description = "사용자를 정지 상태로 전환하고 정지 사유/기간을 기록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정지 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    })
    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable Long userId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody SuspendRequest request) {
        adminService.suspendUser(userId, adminId, request);
        return ResponseEntity.ok(ApiResponse.success("사용자가 정지되었습니다."));
    }

    @Operation(summary = "[관리자] 사용자 정지 해제", description = "정지된 사용자를 정상 상태로 복구합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SUSPENSION_NOT_FOUND, USER_NOT_FOUND")
    })
    @PutMapping("/users/{userId}/unsuspend")
    public ResponseEntity<ApiResponse<Void>> unsuspendUser(
            @PathVariable Long userId,
            @CurrentUserId Long adminId) {
        adminService.unsuspendUser(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success("사용자 정지가 해제되었습니다."));
    }

    @Operation(summary = "[관리자] 게시글 신고 목록", description = "처리 상태(status)로 필터링 가능. 커서 페이지네이션.")
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<CursorPageResponse<ReportListResponse>>> getReports(
            @ModelAttribute CursorPageRequest pageRequest,
            @Parameter(description = "신고 처리 상태 (PENDING, APPROVED, REJECTED)") @RequestParam(required = false) ReportStatus status) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getReports(pageRequest, status)));
    }

    @Operation(summary = "[관리자] 신고 처리", description = "신고를 처리(원본 게시글 삭제, 사용자 정지 등)하고 결과를 기록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "REPORT_NOT_FOUND")
    })
    @PutMapping("/reports/{reportId}")
    public ResponseEntity<ApiResponse<Void>> processReport(
            @PathVariable Long reportId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody ReportProcessRequest request) {
        adminService.processReport(reportId, adminId, request);
        return ResponseEntity.ok(ApiResponse.success("신고가 처리되었습니다."));
    }

    @Operation(summary = "[관리자] 공지 목록 조회", description = "관리용 공지 목록. 일반 사용자용은 /api/v1/notices 사용.")
    @GetMapping("/notices")
    public ResponseEntity<ApiResponse<CursorPageResponse<NoticeListResponse>>> getNotices(
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getList(pageRequest)));
    }

    @Operation(summary = "[관리자] 공지 작성", description = "신규 공지사항을 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공")
    })
    @PostMapping("/notices")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> createNotice(
            @CurrentUserId Long adminId,
            @Valid @RequestBody NoticeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("공지사항이 작성되었습니다.", adminService.createNotice(adminId, request)));
    }

    @Operation(summary = "[관리자] 공지 수정", description = "공지의 제목/본문을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "NOTICE_NOT_FOUND")
    })
    @PutMapping("/notices/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> updateNotice(
            @PathVariable Long noticeId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody NoticeCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("공지사항이 수정되었습니다.", adminService.updateNotice(noticeId, adminId, request)));
    }

    @Operation(summary = "[관리자] 공지 삭제", description = "공지사항을 soft delete 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "NOTICE_NOT_FOUND")
    })
    @DeleteMapping("/notices/{noticeId}")
    public ResponseEntity<Void> deleteNotice(
            @PathVariable Long noticeId) {
        adminService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "[관리자] 문의 목록 조회", description = "처리 상태/문의 유형으로 필터링 가능.")
    @GetMapping("/inquiries")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminInquiryListResponse>>> getInquiries(
            @ModelAttribute CursorPageRequest pageRequest,
            @Parameter(description = "처리 상태 (PENDING, ANSWERED)") @RequestParam(required = false) InquiryStatus status,
            @Parameter(description = "문의 유형") @RequestParam(required = false) InquiryType type) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getInquiries(pageRequest, status, type)));
    }

    @Operation(summary = "[관리자] 문의 답변 등록", description = "문의에 대한 관리자 답변을 등록합니다. 답변 등록 시 사용자에게 알림 발송.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "답변 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INQUIRY_NOT_FOUND")
    })
    @PostMapping("/inquiries/{inquiryId}/reply")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> replyInquiry(
            @PathVariable Long inquiryId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody InquiryReplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("문의 답변이 등록되었습니다.", adminService.replyInquiry(inquiryId, adminId, request)));
    }

    @Operation(summary = "[관리자] 사용자 요청 목록", description = "신규 팀/리그 등록 요청 등의 처리 대기 목록을 조회합니다.")
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminRequestListResponse>>> getRequests(
            @ModelAttribute CursorPageRequest pageRequest,
            @Parameter(description = "처리 상태") @RequestParam(required = false) RequestStatus status,
            @Parameter(description = "요청 유형") @RequestParam(required = false) RequestType type) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getRequests(pageRequest, status, type)));
    }

    @Operation(summary = "[관리자] 요청 처리", description = "사용자 요청을 승인/반려 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "REQUEST_NOT_FOUND")
    })
    @PutMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<RequestDetailResponse>> processRequest(
            @PathVariable Long requestId,
            @CurrentUserId Long adminId,
            @Valid @RequestBody RequestProcessRequest request) {
        return ResponseEntity.ok(ApiResponse.success("요청이 처리되었습니다.", adminService.processRequest(requestId, adminId, request)));
    }
}
