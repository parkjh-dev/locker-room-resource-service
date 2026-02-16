package com.lockerroom.resourceservice.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long id,
        Long boardId,
        String boardName,
        AuthorInfo author,
        String title,
        String content,
        int viewCount,
        int likeCount,
        int commentCount,
        boolean isAiGenerated,
        boolean isLiked,
        List<FileResponse> files,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
