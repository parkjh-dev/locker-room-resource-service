package com.lockerroom.resourceservice.sport.service.impl;

import com.lockerroom.resourceservice.sport.dto.response.SportResponse;
import com.lockerroom.resourceservice.sport.mapper.SportMapper;
import com.lockerroom.resourceservice.sport.model.entity.Sport;
import com.lockerroom.resourceservice.sport.repository.SportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SportServiceImplTest {

    @Mock private SportRepository sportRepository;
    @Mock private com.lockerroom.resourceservice.sport.repository.ContinentRepository continentRepository;
    @Mock private com.lockerroom.resourceservice.sport.repository.CountryRepository countryRepository;
    @Mock private com.lockerroom.resourceservice.sport.repository.FootballLeagueRepository footballLeagueRepository;
    @Mock private com.lockerroom.resourceservice.sport.repository.BaseballLeagueRepository baseballLeagueRepository;
    @Mock private SportMapper sportMapper;

    @InjectMocks private SportServiceImpl sportService;

    private Sport soccer;
    private Sport baseball;

    @BeforeEach
    void setUp() {
        soccer = Sport.builder()
                .id(1L)
                .nameKo("축구")
                .nameEn("Football")
                .isActive(true)
                .build();

        baseball = Sport.builder()
                .id(2L)
                .nameKo("야구")
                .nameEn("Baseball")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("getSports")
    class GetSports {

        @Test
        @DisplayName("should return all active sports")
        void getSports_success() {
            SportResponse soccerResponse = new SportResponse(1L, "축구", "Football", true);
            SportResponse baseballResponse = new SportResponse(2L, "야구", "Baseball", true);

            when(sportRepository.findByIsActiveTrue()).thenReturn(List.of(soccer, baseball));
            when(sportMapper.toSportResponse(soccer)).thenReturn(soccerResponse);
            when(sportMapper.toSportResponse(baseball)).thenReturn(baseballResponse);

            List<SportResponse> result = sportService.getSports();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).nameKo()).isEqualTo("축구");
            assertThat(result.get(1).nameKo()).isEqualTo("야구");
        }

        @Test
        @DisplayName("should return empty list when no active sports")
        void getSports_empty() {
            when(sportRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());

            List<SportResponse> result = sportService.getSports();

            assertThat(result).isEmpty();
        }
    }

    /* ────────── Phase 1: Cascading API ────────── */

    @Nested
    @DisplayName("getCountriesBySport — 종목별 분기")
    class GetCountriesBySport {

        @Test
        @DisplayName("Football → footballLeagueRepository로 distinct country IDs 조회")
        void football_callsFootballRepo() {
            when(sportRepository.findById(1L)).thenReturn(java.util.Optional.of(soccer));
            when(footballLeagueRepository.findDistinctCountryIdsBySport(1L)).thenReturn(java.util.List.of());

            sportService.getCountriesBySport(1L);

            org.mockito.Mockito.verify(footballLeagueRepository).findDistinctCountryIdsBySport(1L);
            org.mockito.Mockito.verify(baseballLeagueRepository, org.mockito.Mockito.never())
                    .findDistinctCountryIdsBySport(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("Baseball → baseballLeagueRepository로 분기")
        void baseball_callsBaseballRepo() {
            when(sportRepository.findById(2L)).thenReturn(java.util.Optional.of(baseball));
            when(baseballLeagueRepository.findDistinctCountryIdsBySport(2L)).thenReturn(java.util.List.of());

            sportService.getCountriesBySport(2L);

            org.mockito.Mockito.verify(baseballLeagueRepository).findDistinctCountryIdsBySport(2L);
            org.mockito.Mockito.verify(footballLeagueRepository, org.mockito.Mockito.never())
                    .findDistinctCountryIdsBySport(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("미지원 종목(농구) → 빈 결과")
        void basketball_returnsEmpty() {
            Sport basketball = Sport.builder().id(3L).nameKo("농구").nameEn("Basketball").build();
            when(sportRepository.findById(3L)).thenReturn(java.util.Optional.of(basketball));

            assertThat(sportService.getCountriesBySport(3L)).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 sport ID → SPORT_NOT_FOUND")
        void unknownSport_throws() {
            when(sportRepository.findById(99L)).thenReturn(java.util.Optional.empty());

            com.lockerroom.resourceservice.infrastructure.exceptions.CustomException ex =
                    org.junit.jupiter.api.Assertions.assertThrows(
                            com.lockerroom.resourceservice.infrastructure.exceptions.CustomException.class,
                            () -> sportService.getCountriesBySport(99L));
            assertThat(ex.getErrorCode())
                    .isEqualTo(com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode.SPORT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getLeaguesByCountryAndSport — 종목별 분기")
    class GetLeaguesByCountryAndSport {

        @Test
        @DisplayName("Football → football leagues 조회")
        void football() {
            when(sportRepository.findById(1L)).thenReturn(java.util.Optional.of(soccer));
            when(footballLeagueRepository.findBySportIdAndCountryId(1L, 1L)).thenReturn(java.util.List.of());

            sportService.getLeaguesByCountryAndSport(1L, 1L);

            org.mockito.Mockito.verify(footballLeagueRepository).findBySportIdAndCountryId(1L, 1L);
        }

        @Test
        @DisplayName("Baseball → baseball leagues 조회")
        void baseballBranch() {
            when(sportRepository.findById(2L)).thenReturn(java.util.Optional.of(baseball));
            when(baseballLeagueRepository.findBySportIdAndCountryId(2L, 1L)).thenReturn(java.util.List.of());

            sportService.getLeaguesByCountryAndSport(2L, 1L);

            org.mockito.Mockito.verify(baseballLeagueRepository).findBySportIdAndCountryId(2L, 1L);
        }

        @Test
        @DisplayName("미지원 종목 → 빈 결과")
        void unsupportedSport_empty() {
            Sport volleyball = Sport.builder().id(4L).nameKo("배구").nameEn("Volleyball").build();
            when(sportRepository.findById(4L)).thenReturn(java.util.Optional.of(volleyball));

            assertThat(sportService.getLeaguesByCountryAndSport(4L, 1L)).isEmpty();
        }
    }
}
