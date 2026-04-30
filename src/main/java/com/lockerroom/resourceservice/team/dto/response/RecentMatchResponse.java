package com.lockerroom.resourceservice.team.dto.response;

import com.lockerroom.resourceservice.team.model.enums.MatchResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "종료된 경기 응답 (최근 N경기).")
public record RecentMatchResponse(
        @Schema(description = "경기 ID", example = "8001")
        Long id,

        @Schema(description = "대회명", example = "K리그1")
        String competitionName,

        @Schema(description = "상대팀")
        OpponentBriefResponse opponent,

        @Schema(description = "홈 경기 여부", example = "false")
        boolean isHome,

        @Schema(description = "우리 팀 점수", example = "2")
        int teamScore,

        @Schema(description = "상대 점수", example = "1")
        int opponentScore,

        @Schema(description = "결과 (WIN/DRAW/LOSS)")
        MatchResult result,

        @Schema(description = "경기 시각 (ISO)", example = "2026-04-25T19:00:00")
        LocalDateTime playedAt
) {
}
