package com.lockerroom.resourceservice.comment.dto.response;

import com.lockerroom.resourceservice.common.dto.response.AuthorInfo;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        AuthorInfo author,
        String content,
        boolean isAiGenerated,
        LocalDateTime createdAt,
        List<CommentResponse> replies
) {
}
