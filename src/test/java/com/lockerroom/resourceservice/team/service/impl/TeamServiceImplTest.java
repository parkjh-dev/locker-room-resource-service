package com.lockerroom.resourceservice.team.service.impl;

import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.sport.model.entity.BaseballLeague;
import com.lockerroom.resourceservice.sport.model.entity.BaseballTeam;
import com.lockerroom.resourceservice.sport.model.entity.FootballLeague;
import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;
import com.lockerroom.resourceservice.sport.repository.BaseballTeamRepository;
import com.lockerroom.resourceservice.sport.repository.FootballTeamRepository;
import com.lockerroom.resourceservice.team.dto.response.TeamDashboardResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock private FootballTeamRepository footballTeamRepository;
    @Mock private BaseballTeamRepository baseballTeamRepository;

    @InjectMocks private TeamServiceImpl teamService;

    @Nested
    @DisplayName("getDashboard")
    class GetDashboard {

        @Test
        @DisplayName("FootballTeam 매칭 — profile 채워지고 stub 필드는 null/빈배열")
        void football_returnsProfileWithStubs() {
            FootballLeague league = FootballLeague.builder()
                    .id(1L).nameKo("K리그1").nameEn("K League 1").build();
            FootballTeam team = FootballTeam.builder()
                    .id(101L).nameKo("전북 현대").nameEn("Jeonbuk Hyundai")
                    .logoUrl("https://cdn/jeonbuk.png").league(league).build();

            when(footballTeamRepository.findById(101L)).thenReturn(Optional.of(team));

            TeamDashboardResponse result = teamService.getDashboard(101L);

            assertThat(result.team().id()).isEqualTo(101L);
            assertThat(result.team().name()).isEqualTo("전북 현대");
            assertThat(result.team().logoUrl()).isEqualTo("https://cdn/jeonbuk.png");
            assertThat(result.team().leagueName()).isEqualTo("K리그1");
            assertThat(result.team().founded()).isNull();   // TODO stub
            assertThat(result.team().venue()).isNull();
            assertThat(result.team().description()).isNull();
            assertThat(result.season()).isEqualTo("2026");
            assertThat(result.nextMatch()).isNull();
            assertThat(result.recentMatches()).isEmpty();
            assertThat(result.standing()).isNull();
        }

        @Test
        @DisplayName("Football에 없으면 BaseballTeam fallback 조회")
        void baseballFallback() {
            BaseballLeague league = BaseballLeague.builder()
                    .id(2L).nameKo("KBO 리그").nameEn("KBO").build();
            BaseballTeam team = BaseballTeam.builder()
                    .id(201L).nameKo("LG 트윈스").nameEn("LG Twins")
                    .league(league).build();

            when(footballTeamRepository.findById(201L)).thenReturn(Optional.empty());
            when(baseballTeamRepository.findById(201L)).thenReturn(Optional.of(team));

            TeamDashboardResponse result = teamService.getDashboard(201L);

            assertThat(result.team().id()).isEqualTo(201L);
            assertThat(result.team().name()).isEqualTo("LG 트윈스");
            assertThat(result.team().leagueName()).isEqualTo("KBO 리그");
        }

        @Test
        @DisplayName("Football·Baseball 모두 매칭 안 되면 RESOURCE_NOT_FOUND")
        void unknownTeam_throws() {
            when(footballTeamRepository.findById(999L)).thenReturn(Optional.empty());
            when(baseballTeamRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> teamService.getDashboard(999L));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("league가 null인 팀이라도 leagueName=null로 응답 (NPE 방어)")
        void teamWithoutLeague_nullLeagueName() {
            FootballTeam team = FootballTeam.builder()
                    .id(101L).nameKo("Test").nameEn("Test").league(null).build();

            when(footballTeamRepository.findById(101L)).thenReturn(Optional.of(team));

            TeamDashboardResponse result = teamService.getDashboard(101L);

            assertThat(result.team().leagueName()).isNull();
        }
    }
}
