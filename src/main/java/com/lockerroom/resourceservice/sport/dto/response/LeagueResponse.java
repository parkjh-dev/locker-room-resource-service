package com.lockerroom.resourceservice.sport.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "리그 응답. 축구·야구 통합 추상 (Football/Baseball League 매핑).")
public record LeagueResponse(
        @Schema(description = "리그 ID", example = "1")
        Long id,

        @Schema(description = "한국어 리그명", example = "K리그1")
        String nameKo,

        @Schema(description = "소속 종목 ID", example = "1")
        Long sportId,

        @Schema(description = "소속 국가 ID", example = "1")
        Long countryId,

        @Schema(description = "리그 티어 (1=1부)", example = "1")
        int tier,

        @Schema(description = "리그 로고 URL", nullable = true)
        String logoUrl
) {
}
