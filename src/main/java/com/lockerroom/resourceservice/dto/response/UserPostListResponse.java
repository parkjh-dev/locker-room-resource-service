package com.lockerroom.resourceservice.dto.response;

import java.time.LocalDateTime;

public record UserPostListResponse(
        Long id,
        Long boardId,
        String boardName,
        String title,
        int viewCount,
        int likeCount,
        int commentCount,
        LocalDateTime createdAt
) {
}
