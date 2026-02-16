package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.response.SportResponse;
import com.lockerroom.resourceservice.dto.response.TeamResponse;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.mapper.PostMapper;
import com.lockerroom.resourceservice.model.entity.Sport;
import com.lockerroom.resourceservice.model.entity.Team;
import com.lockerroom.resourceservice.repository.SportRepository;
import com.lockerroom.resourceservice.repository.TeamRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SportServiceImplTest {

    @Mock private SportRepository sportRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private PostMapper postMapper;

    @InjectMocks private SportServiceImpl sportService;

    private Sport soccer;
    private Sport baseball;
    private Team teamA;
    private Team teamB;

    @BeforeEach
    void setUp() {
        soccer = Sport.builder()
                .id(1L)
                .name("Soccer")
                .isActive(true)
                .build();

        baseball = Sport.builder()
                .id(2L)
                .name("Baseball")
                .isActive(true)
                .build();

        teamA = Team.builder()
                .id(1L)
                .sport(soccer)
                .name("Team A")
                .logoUrl("https://logo.com/a.png")
                .isActive(true)
                .build();

        teamB = Team.builder()
                .id(2L)
                .sport(soccer)
                .name("Team B")
                .logoUrl("https://logo.com/b.png")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("getSports")
    class GetSports {

        @Test
        @DisplayName("should return all active sports")
        void getSports_success() {
            SportResponse soccerResponse = new SportResponse(1L, "Soccer", true);
            SportResponse baseballResponse = new SportResponse(2L, "Baseball", true);

            when(sportRepository.findByIsActiveTrue()).thenReturn(List.of(soccer, baseball));
            when(postMapper.toSportResponse(soccer)).thenReturn(soccerResponse);
            when(postMapper.toSportResponse(baseball)).thenReturn(baseballResponse);

            List<SportResponse> result = sportService.getSports();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Soccer");
            assertThat(result.get(1).name()).isEqualTo("Baseball");
        }

        @Test
        @DisplayName("should return empty list when no active sports")
        void getSports_empty() {
            when(sportRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());

            List<SportResponse> result = sportService.getSports();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTeamsBySport")
    class GetTeamsBySport {

        @Test
        @DisplayName("should return teams for a given sport")
        void getTeamsBySport_success() {
            TeamResponse teamAResponse = new TeamResponse(1L, "Team A", "https://logo.com/a.png", true);
            TeamResponse teamBResponse = new TeamResponse(2L, "Team B", "https://logo.com/b.png", true);

            when(sportRepository.findById(1L)).thenReturn(Optional.of(soccer));
            when(teamRepository.findBySportIdAndIsActiveTrue(1L)).thenReturn(List.of(teamA, teamB));
            when(postMapper.toTeamResponse(teamA)).thenReturn(teamAResponse);
            when(postMapper.toTeamResponse(teamB)).thenReturn(teamBResponse);

            List<TeamResponse> result = sportService.getTeamsBySport(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Team A");
            assertThat(result.get(1).name()).isEqualTo("Team B");
        }

        @Test
        @DisplayName("should return empty list when sport has no active teams")
        void getTeamsBySport_noTeams() {
            when(sportRepository.findById(2L)).thenReturn(Optional.of(baseball));
            when(teamRepository.findBySportIdAndIsActiveTrue(2L)).thenReturn(Collections.emptyList());

            List<TeamResponse> result = sportService.getTeamsBySport(2L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when sport not found")
        void getTeamsBySport_sportNotFound_throwsException() {
            when(sportRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> sportService.getTeamsBySport(999L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SPORT_NOT_FOUND);
        }
    }
}
