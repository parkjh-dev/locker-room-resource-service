package com.lockerroom.resourceservice.notice.dto.response;

import com.lockerroom.resourceservice.notice.model.enums.NoticeScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공지사항 상세 응답.")
public record NoticeDetailResponse(
        @Schema(description = "공지 ID", example = "12")
        Long id,

        @Schema(description = "공지 제목", example = "서비스 점검 안내")
        String title,

        @Schema(description = "공지 본문", example = "5월 1일 0시~2시 점검 예정입니다.")
        String content,

        @Schema(description = "상단 고정 여부", example = "true")
        boolean isPinned,

        @Schema(description = "공지 노출 범위 (ALL=전체, TEAM=특정 팀)")
        NoticeScope scope,

        @Schema(description = "팀 한정 공지일 때 팀 ID. 그 외 null.", example = "101", nullable = true)
        Long teamId,

        @Schema(description = "팀 한정 공지일 때 팀명. 그 외 null.", example = "전북 현대 모터스", nullable = true)
        String teamName,

        @Schema(description = "작성한 관리자 닉네임", example = "운영자")
        String adminNickname,

        @Schema(description = "작성일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt,

        @Schema(description = "최종 수정일시", example = "2026-04-28T10:15:00")
        LocalDateTime updatedAt
) {
}
