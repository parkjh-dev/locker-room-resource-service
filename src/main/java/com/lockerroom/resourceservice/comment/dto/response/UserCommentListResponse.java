package com.lockerroom.resourceservice.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "내가 작성한 댓글 목록 행 응답.")
public record UserCommentListResponse(
        @Schema(description = "댓글 ID", example = "501")
        Long id,

        @Schema(description = "댓글이 달린 게시글 ID", example = "1024")
        Long postId,

        @Schema(description = "게시글 제목", example = "두산 베어스 응원합니다")
        String postTitle,

        @Schema(description = "댓글 내용", example = "좋은 글 잘 봤습니다!")
        String content,

        @Schema(description = "작성일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt
) {
}
