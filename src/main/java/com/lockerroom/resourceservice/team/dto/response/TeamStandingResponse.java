package com.lockerroom.resourceservice.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "리그 순위 정보 응답.")
public record TeamStandingResponse(
        @Schema(description = "현재 순위", example = "3")
        int rank,

        @Schema(description = "전체 팀 수", example = "12")
        int totalTeams,

        @Schema(description = "치른 경기 수", example = "13")
        int matchesPlayed,

        @Schema(description = "승", example = "7")
        int wins,

        @Schema(description = "무 (KBO 등 무승부 없는 종목은 0)", example = "4")
        int draws,

        @Schema(description = "패", example = "2")
        int losses,

        @Schema(description = "승점 (KBO 등 무관한 종목은 0)", example = "25")
        int points,

        @Schema(description = "득점", example = "22")
        int goalsFor,

        @Schema(description = "실점", example = "11")
        int goalsAgainst,

        @Schema(description = "득실차", example = "11")
        int goalDifference
) {
}
