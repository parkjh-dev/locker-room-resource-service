package com.lockerroom.resourceservice.notice.dto.response;

import java.time.LocalDateTime;

public record NoticeListResponse(
        Long id,
        String title,
        boolean isPinned,
        LocalDateTime createdAt
) {
}
