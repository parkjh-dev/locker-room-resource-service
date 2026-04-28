package com.lockerroom.resourceservice.sport.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스포츠 종목 응답.")
public record SportResponse(
        @Schema(description = "종목 ID", example = "2")
        Long id,

        @Schema(description = "한국어 종목명", example = "야구")
        String nameKo,

        @Schema(description = "영문 종목명 (백엔드 분기용 키)", example = "Baseball")
        String nameEn,

        @Schema(description = "활성 여부 (false면 화면 노출 안 됨)", example = "true")
        boolean isActive
) {
}
