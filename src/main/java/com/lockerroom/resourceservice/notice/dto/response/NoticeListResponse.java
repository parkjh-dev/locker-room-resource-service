package com.lockerroom.resourceservice.notice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공지사항 목록 행 응답. 본문 미포함.")
public record NoticeListResponse(
        @Schema(description = "공지 ID", example = "12")
        Long id,

        @Schema(description = "공지 제목", example = "서비스 점검 안내")
        String title,

        @Schema(description = "상단 고정 여부 (목록에서 항상 최상단 노출)", example = "true")
        boolean isPinned,

        @Schema(description = "작성일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt
) {
}
