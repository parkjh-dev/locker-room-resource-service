package com.lockerroom.resourceservice.stats.service.impl;

import com.lockerroom.resourceservice.post.repository.BaseballPostRepository;
import com.lockerroom.resourceservice.post.repository.FootballPostRepository;
import com.lockerroom.resourceservice.sport.model.entity.BaseballTeam;
import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;
import com.lockerroom.resourceservice.sport.model.entity.Sport;
import com.lockerroom.resourceservice.sport.repository.BaseballTeamRepository;
import com.lockerroom.resourceservice.sport.repository.FootballTeamRepository;
import com.lockerroom.resourceservice.sport.repository.SportRepository;
import com.lockerroom.resourceservice.stats.dto.request.TeamRankingMetric;
import com.lockerroom.resourceservice.stats.dto.response.TeamRankingResponse;
import com.lockerroom.resourceservice.user.repository.UserTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock private SportRepository sportRepository;
    @Mock private FootballTeamRepository footballTeamRepository;
    @Mock private BaseballTeamRepository baseballTeamRepository;
    @Mock private FootballPostRepository footballPostRepository;
    @Mock private BaseballPostRepository baseballPostRepository;
    @Mock private UserTeamRepository userTeamRepository;

    @InjectMocks private StatsServiceImpl statsService;

    private Sport football;
    private Sport baseball;
    private FootballTeam jeonbuk;
    private FootballTeam ulsan;
    private BaseballTeam lg;

    @BeforeEach
    void setUp() {
        football = Sport.builder().id(1L).nameKo("축구").nameEn("Football").build();
        baseball = Sport.builder().id(2L).nameKo("야구").nameEn("Baseball").build();

        jeonbuk = FootballTeam.builder().id(101L).nameKo("전북 현대").nameEn("Jeonbuk").build();
        ulsan = FootballTeam.builder().id(102L).nameKo("울산 HD").nameEn("Ulsan").build();
        lg = BaseballTeam.builder().id(201L).nameKo("LG 트윈스").nameEn("LG Twins").build();

        // 자주 쓰는 stub은 lenient (테스트별 사용 안 해도 OK)
        lenient().when(sportRepository.findByNameEnIgnoreCase("Football")).thenReturn(Optional.of(football));
        lenient().when(sportRepository.findByNameEnIgnoreCase("Baseball")).thenReturn(Optional.of(baseball));
    }

    @Nested
    @DisplayName("getTeamRanking — sport 필터")
    class SportFilter {

        @Test
        @DisplayName("sport=축구 — Football 팀만 반환")
        void footballOnly() {
            when(footballTeamRepository.findAll()).thenReturn(List.of(jeonbuk, ulsan));
            when(footballPostRepository.countByTeamSince(any())).thenReturn(List.<Object[]>of());
            when(userTeamRepository.countFollowersBySport(1L)).thenReturn(List.<Object[]>of(
                    new Object[]{101L, 100L}, new Object[]{102L, 50L}));

            List<TeamRankingResponse> result = statsService.getTeamRanking(
                    TeamRankingMetric.FOLLOWERS, "축구", 5);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(r -> "축구".equals(r.sportName()));
        }

        @Test
        @DisplayName("sport=야구 — Baseball 팀만 반환")
        void baseballOnly() {
            when(baseballTeamRepository.findAll()).thenReturn(List.of(lg));
            when(baseballPostRepository.countByTeamSince(any())).thenReturn(List.<Object[]>of());
            when(userTeamRepository.countFollowersBySport(2L)).thenReturn(List.<Object[]>of(
                    new Object[]{201L, 80L}));

            List<TeamRankingResponse> result = statsService.getTeamRanking(
                    TeamRankingMetric.FOLLOWERS, "야구", 5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).sportName()).isEqualTo("야구");
        }

        @Test
        @DisplayName("sport=ALL — Football·Baseball 모두 통합")
        void all_combinesBothSports() {
            when(footballTeamRepository.findAll()).thenReturn(List.of(jeonbuk));
            when(baseballTeamRepository.findAll()).thenReturn(List.of(lg));
            when(footballPostRepository.countByTeamSince(any())).thenReturn(List.<Object[]>of());
            when(baseballPostRepository.countByTeamSince(any())).thenReturn(List.<Object[]>of());
            when(userTeamRepository.countFollowersBySport(1L)).thenReturn(List.<Object[]>of(
                    new Object[]{101L, 100L}));
            when(userTeamRepository.countFollowersBySport(2L)).thenReturn(List.<Object[]>of(
                    new Object[]{201L, 80L}));

            List<TeamRankingResponse> result = statsService.getTeamRanking(
                    TeamRankingMetric.FOLLOWERS, "ALL", 5);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(TeamRankingResponse::sportName)
                    .containsExactlyInAnyOrder("축구", "야구");
        }

        @Test
        @DisplayName("sport=농구 — Football·Baseball 둘 다 미포함 (빈 결과)")
        void basketball_returnsEmpty() {
            List<TeamRankingResponse> result = statsService.getTeamRanking(
                    TeamRankingMetric.FOLLOWERS, "농구", 5);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("sport=배구 — 빈 결과")
        void volleyball_returnsEmpty() {
            List<TeamRankingResponse> result = statsService.getTeamRanking(
                    TeamRankingMetric.FOLLOWERS, "배구", 5);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTeamRanking — 정렬 및 rank")
    class SortAndRank {

        @Test
        @DisplayName("FOLLOWERS — 응원자 수 내림차순, rank 1부터 부여")
        void followers_sortedDesc() {
            when(footballTeamRepository.findAll()).thenReturn(List.of(jeonbuk, ulsan));
            when(footballPostRepository.countByTeamSince(any())).thenReturn(List.<Object[]>of());
            when(userTeamRepository.countFollowersBySport(1L)).thenReturn(List.<Object[]>of(
                    new Object[]{101L, 100L}, // 1위 후보
                    new Object[]{102L, 50L}));

            List<TeamRankingResponse> result = statsService.getTeamRanking(
                    TeamRankingMetric.FOLLOWERS, "축구", 5);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).rank()).isEqualTo(1);
            assertThat(result.get(0).team().id()).isEqualTo(101L);
            assertThat(result.get(0).followerCount()).isEqualTo(100L);
            assertThat(result.get(1).rank()).isEqualTo(2);
            assertThat(result.get(1).team().id()).isEqualTo(102L);
        }

        @Test
        @DisplayName("AVG_POSTS — 일평균 글 수 내림차순")
        void avgPosts_sortedDesc() {
            when(footballTeamRepository.findAll()).thenReturn(List.of(jeonbuk, ulsan));
            // 30일에 600건 → 20/day, 30건 → 1/day
            when(footballPostRepository.countByTeamSince(any())).thenReturn(List.<Object[]>of(
                    new Object[]{102L, 600L}, // ulsan: 20/day
                    new Object[]{101L, 30L}   // jeonbuk: 1/day
            ));
            when(userTeamRepository.countFollowersBySport(1L)).thenReturn(List.<Object[]>of());

            List<TeamRankingResponse> result = statsService.getTeamRanking(
                    TeamRankingMetric.AVG_POSTS, "축구", 5);

            assertThat(result.get(0).team().id()).isEqualTo(102L); // ulsan 1위
            assertThat(result.get(0).avgPostsPerDay()).isEqualTo(20);
            assertThat(result.get(1).team().id()).isEqualTo(101L);
        }

        @Test
        @DisplayName("size로 잘림 — top N만")
        void size_limitsResult() {
            when(footballTeamRepository.findAll()).thenReturn(List.of(jeonbuk, ulsan));
            when(footballPostRepository.countByTeamSince(any())).thenReturn(List.<Object[]>of());
            when(userTeamRepository.countFollowersBySport(1L)).thenReturn(List.<Object[]>of(
                    new Object[]{101L, 100L}, new Object[]{102L, 50L}));

            List<TeamRankingResponse> result = statsService.getTeamRanking(
                    TeamRankingMetric.FOLLOWERS, "축구", 1);

            assertThat(result).hasSize(1);
        }
    }
}
