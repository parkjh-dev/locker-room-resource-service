package com.lockerroom.resourceservice.stats.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "팀 랭킹 행. metric 기준 내림차순으로 백엔드가 rank 부여.")
public record TeamRankingResponse(
        @Schema(description = "순위 (1부터)", example = "1")
        int rank,

        @Schema(description = "팀 정보")
        TeamSummaryResponse team,

        @Schema(description = "종목명 (한국어)", example = "축구")
        String sportName,

        @Schema(description = "응원자 수", example = "12400")
        long followerCount,

        @Schema(description = "최근 30일 일평균 게시글 수", example = "23")
        int avgPostsPerDay
) implements Serializable {
}
