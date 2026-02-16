package com.lockerroom.resourceservice.service;

import com.lockerroom.resourceservice.dto.request.*;
import com.lockerroom.resourceservice.dto.response.*;

public interface AdminService {

    CursorPageResponse<AdminUserListResponse> getUsers(CursorPageRequest pageRequest);

    void suspendUser(Long userId, Long adminId, SuspendRequest request);

    CursorPageResponse<ReportListResponse> getReports(CursorPageRequest pageRequest);

    void processReport(Long reportId, Long adminId, ReportProcessRequest request);

    NoticeDetailResponse createNotice(Long adminId, NoticeCreateRequest request);

    NoticeDetailResponse updateNotice(Long noticeId, Long adminId, NoticeCreateRequest request);

    void deleteNotice(Long noticeId);

    CursorPageResponse<AdminInquiryListResponse> getInquiries(CursorPageRequest pageRequest);

    InquiryDetailResponse replyInquiry(Long inquiryId, Long adminId, InquiryReplyRequest request);

    CursorPageResponse<AdminRequestListResponse> getRequests(CursorPageRequest pageRequest);

    RequestDetailResponse processRequest(Long requestId, Long adminId, RequestProcessRequest request);
}
