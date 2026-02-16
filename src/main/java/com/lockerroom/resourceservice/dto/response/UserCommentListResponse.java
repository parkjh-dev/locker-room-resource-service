package com.lockerroom.resourceservice.dto.response;

import java.time.LocalDateTime;

public record UserCommentListResponse(
        Long id,
        Long postId,
        String postTitle,
        String content,
        LocalDateTime createdAt
) {
}
