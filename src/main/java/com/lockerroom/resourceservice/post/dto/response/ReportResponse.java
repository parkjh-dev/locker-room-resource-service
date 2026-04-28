package com.lockerroom.resourceservice.post.dto.response;

import com.lockerroom.resourceservice.post.model.enums.ReportStatus;

public record ReportResponse(
        Long reportId,
        Long postId,
        ReportStatus status
) {
}
