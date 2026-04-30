package com.lockerroom.resourceservice.team.service.impl;

import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.sport.model.entity.BaseballTeam;
import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;
import com.lockerroom.resourceservice.sport.repository.BaseballTeamRepository;
import com.lockerroom.resourceservice.sport.repository.FootballTeamRepository;
import com.lockerroom.resourceservice.team.dto.response.TeamDashboardResponse;
import com.lockerroom.resourceservice.team.dto.response.TeamProfileResponse;
import com.lockerroom.resourceservice.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    /**
     * TODO 시즌 라벨을 동적으로 결정 (현재는 하드코딩).
     *  - 종목·리그별 시즌 시작·종료 시점이 다름
     *  - 시즌 메타데이터를 별도 테이블로 관리하거나 외부 데이터 API에서 조회 필요
     */
    private static final String CURRENT_SEASON = "2026";

    private final FootballTeamRepository footballTeamRepository;
    private final BaseballTeamRepository baseballTeamRepository;

    @Override
    public TeamDashboardResponse getDashboard(Long teamId) {
        TeamProfileResponse profile = findProfile(teamId);
        if (profile == null) {
            // 농구·배구는 entity 부재, 매칭 없는 팀은 404
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // TODO §4: Match·Standing 도메인 신설 후 실데이터로 교체.
        //   1. nextMatch    — 외부 스포츠 데이터 API (K리그/KBO 공식 OPI 등) 또는 운영 어드민 입력
        //   2. recentMatches — 동일 소스에서 최근 5경기
        //   3. standing     — 시즌 순위
        //   현재는 프론트에 null/빈 배열을 반환해 "데이터 없음" UI로 fallback 되도록 함.
        return new TeamDashboardResponse(
                profile,
                CURRENT_SEASON,
                null,           // nextMatch
                List.of(),      // recentMatches
                null            // standing
        );
    }

    private TeamProfileResponse findProfile(Long teamId) {
        return footballTeamRepository.findById(teamId)
                .map(this::toProfile)
                .orElseGet(() -> baseballTeamRepository.findById(teamId)
                        .map(this::toProfile)
                        .orElse(null));
    }

    /**
     * TODO §4: founded·venue·description 컬럼이 entity에 부재.
     *  Football/BaseballTeam에 컬럼 추가 또는 별도 TeamProfile 메타 테이블 신설 필요.
     *  현재는 null 반환 (프론트가 "정보 준비 중" 처리).
     */
    private TeamProfileResponse toProfile(FootballTeam team) {
        return new TeamProfileResponse(
                team.getId(),
                team.getNameKo(),
                team.getLogoUrl(),
                team.getLeague() != null ? team.getLeague().getNameKo() : null,
                null,   // TODO founded
                null,   // TODO venue
                null    // TODO description
        );
    }

    private TeamProfileResponse toProfile(BaseballTeam team) {
        return new TeamProfileResponse(
                team.getId(),
                team.getNameKo(),
                team.getLogoUrl(),
                team.getLeague() != null ? team.getLeague().getNameKo() : null,
                null,   // TODO founded
                null,   // TODO venue
                null    // TODO description
        );
    }
}
