package com.lockerroom.resourceservice.comment.dto.response;

import com.lockerroom.resourceservice.common.dto.response.AuthorInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "댓글 응답. replies 배열에 답글 포함 (depth=1까지만).")
public record CommentResponse(
        @Schema(description = "댓글 ID", example = "501")
        Long id,

        @Schema(description = "작성자 정보")
        AuthorInfo author,

        @Schema(description = "댓글 내용", example = "좋은 글 잘 봤습니다!")
        String content,

        @Schema(description = "AI 자동 생성 댓글 여부", example = "false")
        boolean isAiGenerated,

        @Schema(description = "작성일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt,

        @Schema(description = "답글 목록 (depth=1 한정). 답글이 없으면 빈 배열.")
        List<CommentResponse> replies
) {
}
