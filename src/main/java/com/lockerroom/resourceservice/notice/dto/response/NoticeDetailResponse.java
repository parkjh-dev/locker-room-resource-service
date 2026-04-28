package com.lockerroom.resourceservice.notice.dto.response;

import java.time.LocalDateTime;

public record NoticeDetailResponse(
        Long id,
        String title,
        String content,
        boolean isPinned,
        String adminNickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
