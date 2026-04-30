package com.lockerroom.resourceservice.post.dto.response;

import com.lockerroom.resourceservice.post.model.enums.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "게시글 목록 행 응답 (요약). 본문·투표 상세는 미포함.")
public record PostListResponse(
        @Schema(description = "게시글 ID", example = "1024")
        Long id,

        @Schema(description = "제목", example = "두산 베어스 응원합니다")
        String title,

        @Schema(description = "작성자 닉네임", example = "야구사랑러")
        String authorNickname,

        @Schema(description = "말머리(카테고리)")
        PostCategory category,

        @Schema(description = "투표 포함 여부 (목록 카드의 📊 아이콘 표시용)", example = "false")
        boolean hasPoll,

        @Schema(description = "조회수", example = "1234")
        int viewCount,

        @Schema(description = "좋아요 수", example = "42")
        int likeCount,

        @Schema(description = "댓글 수", example = "8")
        int commentCount,

        @Schema(description = "AI 자동 생성 글 여부", example = "false")
        boolean isAiGenerated,

        @Schema(description = "작성일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt
) {
}
