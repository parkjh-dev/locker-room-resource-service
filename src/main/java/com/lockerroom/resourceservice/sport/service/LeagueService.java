package com.lockerroom.resourceservice.sport.service;

import com.lockerroom.resourceservice.sport.dto.response.TeamResponse;

import java.util.List;

public interface LeagueService {

    /**
     * 리그별 소속 팀 목록.
     * - leagueId가 FootballLeague에 속하면 FootballTeam, BaseballLeague면 BaseballTeam을 조회.
     * - 매칭되는 리그가 없으면 LEAGUE_NOT_FOUND 예외.
     */
    List<TeamResponse> getTeamsByLeague(Long leagueId);
}
