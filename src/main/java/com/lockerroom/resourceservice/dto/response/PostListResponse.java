package com.lockerroom.resourceservice.dto.response;

import java.time.LocalDateTime;

public record PostListResponse(
        Long id,
        String title,
        String authorNickname,
        int viewCount,
        int likeCount,
        int commentCount,
        boolean isAiGenerated,
        LocalDateTime createdAt
) {
}
