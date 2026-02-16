package com.lockerroom.resourceservice.dto.response;

import com.lockerroom.resourceservice.model.enums.NoticeScope;

import java.time.LocalDateTime;

public record NoticeDetailResponse(
        Long id,
        String title,
        String content,
        boolean isPinned,
        NoticeScope scope,
        Long teamId,
        String teamName,
        String adminNickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
