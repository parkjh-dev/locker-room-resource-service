package com.lockerroom.resourceservice.post.dto.response;

import com.lockerroom.resourceservice.post.model.enums.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "[관리자] 신고 목록 행 응답.")
public record ReportListResponse(
        @Schema(description = "신고 ID", example = "55")
        Long id,

        @Schema(description = "신고된 게시글 ID", example = "1024")
        Long postId,

        @Schema(description = "신고된 게시글 제목", example = "부적절한 게시글")
        String postTitle,

        @Schema(description = "신고자 닉네임", example = "신고자")
        String reporterNickname,

        @Schema(description = "신고 사유", example = "욕설 포함")
        String reason,

        @Schema(description = "처리 상태 (PENDING, PROCESSED, DISMISSED)")
        ReportStatus status,

        @Schema(description = "신고 접수 일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt
) {
}
