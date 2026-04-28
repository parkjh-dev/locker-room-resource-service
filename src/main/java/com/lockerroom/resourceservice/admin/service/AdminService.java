package com.lockerroom.resourceservice.admin.service;

import com.lockerroom.resourceservice.admin.dto.response.AdminDashboardResponse;

import com.lockerroom.resourceservice.admin.dto.request.ReportProcessRequest;

import com.lockerroom.resourceservice.admin.dto.request.SuspendRequest;

import com.lockerroom.resourceservice.post.dto.response.ReportListResponse;

import com.lockerroom.resourceservice.post.model.enums.ReportStatus;

import com.lockerroom.resourceservice.user.dto.response.AdminUserListResponse;

import com.lockerroom.resourceservice.inquiry.dto.response.AdminInquiryListResponse;

import com.lockerroom.resourceservice.inquiry.dto.response.InquiryDetailResponse;

import com.lockerroom.resourceservice.inquiry.dto.request.InquiryReplyRequest;

import com.lockerroom.resourceservice.inquiry.model.enums.InquiryType;

import com.lockerroom.resourceservice.inquiry.model.enums.InquiryStatus;

import com.lockerroom.resourceservice.notice.dto.response.NoticeDetailResponse;

import com.lockerroom.resourceservice.notice.dto.request.NoticeCreateRequest;

import com.lockerroom.resourceservice.request.dto.response.AdminRequestListResponse;

import com.lockerroom.resourceservice.request.dto.response.RequestDetailResponse;

import com.lockerroom.resourceservice.request.dto.request.RequestProcessRequest;

import com.lockerroom.resourceservice.request.model.enums.RequestType;

import com.lockerroom.resourceservice.request.model.enums.RequestStatus;

import com.lockerroom.resourceservice.common.model.enums.Role;

import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;


public interface AdminService {

    AdminDashboardResponse getDashboard();

    CursorPageResponse<AdminUserListResponse> getUsers(CursorPageRequest pageRequest, String keyword, Role role);

    void suspendUser(Long userId, Long adminId, SuspendRequest request);

    void unsuspendUser(Long userId, Long adminId);

    CursorPageResponse<ReportListResponse> getReports(CursorPageRequest pageRequest, ReportStatus status);

    void processReport(Long reportId, Long adminId, ReportProcessRequest request);

    NoticeDetailResponse createNotice(Long adminId, NoticeCreateRequest request);

    NoticeDetailResponse updateNotice(Long noticeId, Long adminId, NoticeCreateRequest request);

    void deleteNotice(Long noticeId);

    CursorPageResponse<AdminInquiryListResponse> getInquiries(CursorPageRequest pageRequest, InquiryStatus status, InquiryType type);

    InquiryDetailResponse replyInquiry(Long inquiryId, Long adminId, InquiryReplyRequest request);

    CursorPageResponse<AdminRequestListResponse> getRequests(CursorPageRequest pageRequest, RequestStatus status, RequestType type);

    RequestDetailResponse processRequest(Long requestId, Long adminId, RequestProcessRequest request);
}
