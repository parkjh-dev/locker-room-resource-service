package com.lockerroom.resourceservice.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팀 프로필 (대시보드 헤더용).")
public record TeamProfileResponse(
        @Schema(description = "팀 ID", example = "101")
        Long id,

        @Schema(description = "팀명", example = "전북 현대 모터스")
        String name,

        @Schema(description = "팀 로고 URL", nullable = true)
        String logoUrl,

        @Schema(description = "소속 리그 한국어명", example = "K리그1")
        String leagueName,

        @Schema(description = "창단 연도. 데이터 없으면 null.", example = "1994", nullable = true)
        Integer founded,

        @Schema(description = "홈구장. 데이터 없으면 null.", example = "전주월드컵경기장", nullable = true)
        String venue,

        @Schema(description = "팀 소개 (한두 문단). 미등록 시 null.", nullable = true)
        String description
) {
}
