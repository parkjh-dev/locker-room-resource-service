package com.lockerroom.resourceservice.team.service;

import com.lockerroom.resourceservice.team.dto.response.TeamDashboardResponse;

public interface TeamService {

    /**
     * 팀 게시판 헤더용 대시보드 조회.
     * - 팀 프로필은 FootballTeam/BaseballTeam에서 조회한 실제 데이터
     * - 다음 경기·최근 경기·순위는 TODO (stub: null/empty)
     */
    TeamDashboardResponse getDashboard(Long teamId);
}
