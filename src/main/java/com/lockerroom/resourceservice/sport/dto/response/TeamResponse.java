package com.lockerroom.resourceservice.sport.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팀 응답. 축구·야구 통합 추상 (Football/Baseball Team 매핑).")
public record TeamResponse(
        @Schema(description = "팀 ID", example = "101")
        Long id,

        @Schema(description = "팀명 (한국어)", example = "전북 현대 모터스")
        String name,

        @Schema(description = "팀 로고 URL", nullable = true)
        String logoUrl,

        @Schema(description = "활성 여부", example = "true")
        boolean isActive,

        @Schema(description = "소속 리그 ID", example = "1")
        Long leagueId
) {
}
