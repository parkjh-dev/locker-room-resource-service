package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.response.SportResponse;
import com.lockerroom.resourceservice.dto.response.TeamResponse;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.mapper.PostMapper;
import com.lockerroom.resourceservice.repository.SportRepository;
import com.lockerroom.resourceservice.repository.TeamRepository;
import com.lockerroom.resourceservice.service.SportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SportServiceImpl implements SportService {

    private final SportRepository sportRepository;
    private final TeamRepository teamRepository;
    private final PostMapper postMapper;

    @Override
    public List<SportResponse> getSports() {
        return sportRepository.findByIsActiveTrue().stream()
                .map(postMapper::toSportResponse)
                .toList();
    }

    @Override
    public List<TeamResponse> getTeamsBySport(Long sportId) {
        sportRepository.findById(sportId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPORT_NOT_FOUND));

        return teamRepository.findBySportIdAndIsActiveTrue(sportId).stream()
                .map(postMapper::toTeamResponse)
                .toList();
    }
}
