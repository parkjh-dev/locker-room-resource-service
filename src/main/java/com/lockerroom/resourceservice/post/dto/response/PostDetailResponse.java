package com.lockerroom.resourceservice.post.dto.response;

import com.lockerroom.resourceservice.file.dto.response.FileResponse;

import com.lockerroom.resourceservice.common.dto.response.AuthorInfo;

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
