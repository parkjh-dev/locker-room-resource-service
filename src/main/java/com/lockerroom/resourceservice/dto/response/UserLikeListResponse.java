package com.lockerroom.resourceservice.dto.response;

import java.time.LocalDateTime;

public record UserLikeListResponse(
        Long id,
        Long boardId,
        String boardName,
        String title,
        String authorNickname,
        int viewCount,
        int likeCount,
        int commentCount,
        LocalDateTime createdAt
) {
}
