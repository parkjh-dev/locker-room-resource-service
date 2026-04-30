package com.lockerroom.resourceservice.stats.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팀 요약 (랭킹 응답 안에서 사용).")
public record TeamSummaryResponse(
        @Schema(description = "팀 ID", example = "101")
        Long id,

        @Schema(description = "팀명", example = "전북 현대 모터스")
        String name,

        @Schema(description = "팀 로고 URL", nullable = true)
        String logoUrl
) {
}
