package com.lockerroom.resourceservice.dto.response;

public record AdminDashboardResponse(
        long pendingReportCount,
        long pendingInquiryCount,
        long pendingRequestCount
) {
}
