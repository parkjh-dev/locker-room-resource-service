package com.lockerroom.resourceservice.sport.service.impl;

import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.sport.dto.response.ContinentResponse;
import com.lockerroom.resourceservice.sport.dto.response.CountryResponse;
import com.lockerroom.resourceservice.sport.dto.response.LeagueResponse;
import com.lockerroom.resourceservice.sport.dto.response.SportResponse;
import com.lockerroom.resourceservice.sport.mapper.SportMapper;
import com.lockerroom.resourceservice.sport.model.entity.Sport;
import com.lockerroom.resourceservice.sport.repository.BaseballLeagueRepository;
import com.lockerroom.resourceservice.sport.repository.ContinentRepository;
import com.lockerroom.resourceservice.sport.repository.CountryRepository;
import com.lockerroom.resourceservice.sport.repository.FootballLeagueRepository;
import com.lockerroom.resourceservice.sport.repository.SportRepository;
import com.lockerroom.resourceservice.sport.service.SportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SportServiceImpl implements SportService {

    private static final String SPORT_FOOTBALL = "Football";
    private static final String SPORT_BASEBALL = "Baseball";

    private final SportRepository sportRepository;
    private final ContinentRepository continentRepository;
    private final CountryRepository countryRepository;
    private final FootballLeagueRepository footballLeagueRepository;
    private final BaseballLeagueRepository baseballLeagueRepository;
    private final SportMapper sportMapper;

    @Override
    public List<SportResponse> getSports() {
        return sportRepository.findByIsActiveTrue().stream()
                .map(sportMapper::toSportResponse)
                .toList();
    }

    @Override
    public List<ContinentResponse> getContinents() {
        return continentRepository.findAll().stream()
                .map(sportMapper::toContinentResponse)
                .toList();
    }

    @Override
    public List<CountryResponse> getCountriesBySport(Long sportId) {
        Sport sport = sportRepository.findById(sportId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPORT_NOT_FOUND));

        List<Long> countryIds = switch (sport.getNameEn()) {
            case SPORT_FOOTBALL -> footballLeagueRepository.findDistinctCountryIdsBySport(sportId);
            case SPORT_BASEBALL -> baseballLeagueRepository.findDistinctCountryIdsBySport(sportId);
            default -> Collections.emptyList(); // 농구·배구 등 미지원 종목
        };

        if (countryIds.isEmpty()) return List.of();

        return countryRepository.findAllById(countryIds).stream()
                .map(sportMapper::toCountryResponse)
                // 한국(KR) 우선 정렬
                .sorted(Comparator.comparing((CountryResponse c) -> !"KR".equals(c.code()))
                        .thenComparing(CountryResponse::nameKo))
                .toList();
    }

    @Override
    public List<LeagueResponse> getLeaguesByCountryAndSport(Long sportId, Long countryId) {
        Sport sport = sportRepository.findById(sportId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPORT_NOT_FOUND));

        return switch (sport.getNameEn()) {
            case SPORT_FOOTBALL -> footballLeagueRepository
                    .findBySportIdAndCountryId(sportId, countryId).stream()
                    .map(sportMapper::toLeagueResponse)
                    .toList();
            case SPORT_BASEBALL -> baseballLeagueRepository
                    .findBySportIdAndCountryId(sportId, countryId).stream()
                    .map(sportMapper::toBaseballLeagueResponse)
                    .toList();
            default -> List.of(); // 미지원 종목
        };
    }
}
