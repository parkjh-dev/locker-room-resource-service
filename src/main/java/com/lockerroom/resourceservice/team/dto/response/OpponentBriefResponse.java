package com.lockerroom.resourceservice.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상대팀 요약 (경기 응답에서 사용).")
public record OpponentBriefResponse(
        @Schema(description = "상대팀 ID", example = "102")
        Long id,

        @Schema(description = "상대팀명", example = "울산 HD FC")
        String name,

        @Schema(description = "상대팀 로고 URL", nullable = true)
        String logoUrl
) {
}
