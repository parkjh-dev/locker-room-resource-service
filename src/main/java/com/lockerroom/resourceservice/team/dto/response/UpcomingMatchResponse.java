package com.lockerroom.resourceservice.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "예정된 경기 응답.")
public record UpcomingMatchResponse(
        @Schema(description = "경기 ID", example = "9001")
        Long id,

        @Schema(description = "대회명 (라운드 포함)", example = "K리그1 14R")
        String competitionName,

        @Schema(description = "상대팀")
        OpponentBriefResponse opponent,

        @Schema(description = "홈 경기 여부", example = "true")
        boolean isHome,

        @Schema(description = "경기장", example = "전주월드컵경기장")
        String venue,

        @Schema(description = "킥오프 시각 (ISO)", example = "2026-05-02T19:00:00")
        LocalDateTime kickoffAt
) {
}
