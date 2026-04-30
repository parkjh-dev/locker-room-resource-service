package com.lockerroom.resourceservice.stats.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팀 랭킹 정렬 기준.")
public enum TeamRankingMetric {
    @Schema(description = "응원자(팔로워) 수")
    FOLLOWERS,

    @Schema(description = "최근 30일 일평균 게시글 수")
    AVG_POSTS
}
