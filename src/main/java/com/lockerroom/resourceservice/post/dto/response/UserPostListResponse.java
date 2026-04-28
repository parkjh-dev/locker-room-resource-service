package com.lockerroom.resourceservice.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "내가 작성한 글 목록 행 응답.")
public record UserPostListResponse(
        @Schema(description = "게시글 ID", example = "1024")
        Long id,

        @Schema(description = "소속 게시판 ID", example = "1")
        Long boardId,

        @Schema(description = "소속 게시판 이름", example = "공통 게시판")
        String boardName,

        @Schema(description = "제목", example = "두산 베어스 응원합니다")
        String title,

        @Schema(description = "조회수", example = "1234")
        int viewCount,

        @Schema(description = "좋아요 수", example = "42")
        int likeCount,

        @Schema(description = "댓글 수", example = "8")
        int commentCount,

        @Schema(description = "작성일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt
) {
}
