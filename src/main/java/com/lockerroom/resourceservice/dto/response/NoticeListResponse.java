package com.lockerroom.resourceservice.dto.response;

import com.lockerroom.resourceservice.model.enums.NoticeScope;

import java.time.LocalDateTime;

public record NoticeListResponse(
        Long id,
        String title,
        boolean isPinned,
        NoticeScope scope,
        String teamName,
        LocalDateTime createdAt
) {
}
