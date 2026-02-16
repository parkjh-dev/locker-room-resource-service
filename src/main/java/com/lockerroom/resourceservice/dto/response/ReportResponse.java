package com.lockerroom.resourceservice.dto.response;

import com.lockerroom.resourceservice.model.enums.ReportStatus;

public record ReportResponse(
        Long reportId,
        Long postId,
        ReportStatus status
) {
}
