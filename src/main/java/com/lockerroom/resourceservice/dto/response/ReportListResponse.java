package com.lockerroom.resourceservice.dto.response;

import com.lockerroom.resourceservice.model.enums.ReportStatus;

import java.time.LocalDateTime;

public record ReportListResponse(
        Long id,
        Long postId,
        String postTitle,
        String reporterNickname,
        String reason,
        ReportStatus status,
        LocalDateTime createdAt
) {
}
