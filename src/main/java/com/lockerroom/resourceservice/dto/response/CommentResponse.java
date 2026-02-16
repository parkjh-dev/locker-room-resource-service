package com.lockerroom.resourceservice.dto.response;

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
