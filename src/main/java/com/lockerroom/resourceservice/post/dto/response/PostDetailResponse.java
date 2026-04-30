package com.lockerroom.resourceservice.post.dto.response;

import com.lockerroom.resourceservice.common.dto.response.AuthorInfo;
import com.lockerroom.resourceservice.file.dto.response.FileResponse;
import com.lockerroom.resourceservice.post.model.enums.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "게시글 상세 응답. 본문·말머리·투표·첨부·좋아요 상태 포함.")
public record PostDetailResponse(
        @Schema(description = "게시글 ID", example = "1024")
        Long id,

        @Schema(description = "소속 게시판 ID", example = "1")
        Long boardId,

        @Schema(description = "소속 게시판 이름", example = "공통 게시판")
        String boardName,

        @Schema(description = "작성자 정보")
        AuthorInfo author,

        @Schema(description = "제목", example = "두산 베어스 응원합니다")
        String title,

        @Schema(description = "본문 (Tiptap HTML)", example = "<p>오늘 경기 정말 좋았습니다.</p>")
        String content,

        @Schema(description = "말머리(카테고리)")
        PostCategory category,

        @Schema(description = "투표 정보. 투표 없는 글은 null. 마감 후에도 결과 유지.", nullable = true)
        PollResponse poll,

        @Schema(description = "조회수", example = "1234")
        int viewCount,

        @Schema(description = "좋아요 누적 수", example = "42")
        int likeCount,

        @Schema(description = "댓글 수", example = "8")
        int commentCount,

        @Schema(description = "AI 자동 생성 글 여부", example = "false")
        boolean isAiGenerated,

        @Schema(description = "현재 사용자의 좋아요 여부. 익명 사용자는 항상 false.", example = "true")
        boolean isLiked,

        @Schema(description = "첨부파일 목록")
        List<FileResponse> files,

        @Schema(description = "작성일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt,

        @Schema(description = "최종 수정일시", example = "2026-04-28T10:15:00")
        LocalDateTime updatedAt
) {
}
