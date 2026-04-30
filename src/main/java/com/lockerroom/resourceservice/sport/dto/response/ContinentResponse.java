package com.lockerroom.resourceservice.sport.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대륙 응답.")
public record ContinentResponse(
        @Schema(description = "대륙 ID", example = "1")
        Long id,

        @Schema(description = "한국어명", example = "아시아")
        String nameKo,

        @Schema(description = "ISO 영문/약식 코드", example = "AS")
        String code
) {
}
