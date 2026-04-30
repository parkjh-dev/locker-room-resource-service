package com.lockerroom.resourceservice.sport.service.impl;

import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.sport.dto.response.TeamResponse;
import com.lockerroom.resourceservice.sport.mapper.SportMapper;
import com.lockerroom.resourceservice.sport.repository.BaseballLeagueRepository;
import com.lockerroom.resourceservice.sport.repository.BaseballTeamRepository;
import com.lockerroom.resourceservice.sport.repository.FootballLeagueRepository;
import com.lockerroom.resourceservice.sport.repository.FootballTeamRepository;
import com.lockerroom.resourceservice.sport.service.LeagueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LeagueServiceImpl implements LeagueService {

    private final FootballLeagueRepository footballLeagueRepository;
    private final BaseballLeagueRepository baseballLeagueRepository;
    private final FootballTeamRepository footballTeamRepository;
    private final BaseballTeamRepository baseballTeamRepository;
    private final SportMapper sportMapper;

    @Override
    public List<TeamResponse> getTeamsByLeague(Long leagueId) {
        // FootballLeague에서 먼저 찾고, 없으면 BaseballLeague
        if (footballLeagueRepository.existsById(leagueId)) {
            return footballTeamRepository.findByLeagueId(leagueId).stream()
                    .map(sportMapper::toTeamResponse)
                    .toList();
        }
        if (baseballLeagueRepository.existsById(leagueId)) {
            return baseballTeamRepository.findByLeagueId(leagueId).stream()
                    .map(sportMapper::toBaseballTeamResponse)
                    .toList();
        }
        throw new CustomException(ErrorCode.LEAGUE_NOT_FOUND);
    }
}
