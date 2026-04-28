package com.lockerroom.resourceservice.admin.dto.response;

public record AdminDashboardResponse(
        long pendingReportCount,
        long pendingInquiryCount,
        long pendingRequestCount
) {
}
