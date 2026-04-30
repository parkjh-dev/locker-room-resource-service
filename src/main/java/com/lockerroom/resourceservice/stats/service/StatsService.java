package com.lockerroom.resourceservice.stats.service;

import com.lockerroom.resourceservice.stats.dto.request.TeamRankingMetric;
import com.lockerroom.resourceservice.stats.dto.response.TeamRankingResponse;

import java.util.List;

public interface StatsService {

    /**
     * 팀 랭킹 — metric 기준 내림차순 Top N.
     * @param metric FOLLOWERS | AVG_POSTS
     * @param sport  ALL | 축구 | 야구 | 농구 | 배구 (농구·배구는 entity 부재 → 빈 결과)
     * @param size   1~10
     */
    List<TeamRankingResponse> getTeamRanking(TeamRankingMetric metric, String sport, int size);
}
