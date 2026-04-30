package com.lockerroom.resourceservice.sport.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "국가 응답. 종목별 cascading 응답에 사용.")
public record CountryResponse(
        @Schema(description = "국가 ID", example = "1")
        Long id,

        @Schema(description = "한국어 국가명", example = "대한민국")
        String nameKo,

        @Schema(description = "ISO 코드 (한국=KR, 잉글랜드=GB-ENG 등)", example = "KR")
        String code,

        @Schema(description = "소속 대륙 ID", example = "1")
        Long continentId
) {
}
