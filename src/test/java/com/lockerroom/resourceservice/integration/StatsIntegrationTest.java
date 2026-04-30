package com.lockerroom.resourceservice.integration;

import com.lockerroom.resourceservice.integration.support.IntegrationTestBase;
import com.lockerroom.resourceservice.sport.model.entity.BaseballLeague;
import com.lockerroom.resourceservice.sport.model.entity.BaseballTeam;
import com.lockerroom.resourceservice.sport.model.entity.Continent;
import com.lockerroom.resourceservice.sport.model.entity.Country;
import com.lockerroom.resourceservice.sport.model.entity.FootballLeague;
import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;
import com.lockerroom.resourceservice.sport.model.entity.Sport;
import com.lockerroom.resourceservice.user.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 팀 랭킹 통계 통합 테스트.
 *
 * <p>핵심 검증:
 * <ul>
 *   <li>실제 GROUP BY 쿼리({@code countFollowersBySport})가 MariaDB에서 N+1 없이 동작</li>
 *   <li>FOLLOWERS metric — 응원자 수 내림차순 정렬·rank 부여</li>
 *   <li>sport=ALL 시 Football·Baseball 결과 통합 후 정렬</li>
 *   <li>sport 필터 — "축구" / "야구" 한국어 키 모두 인식</li>
 *   <li>@Cacheable — 같은 인자로 두 번 호출 시 두 번째는 캐시에서 반환</li>
 * </ul>
 */
class StatsIntegrationTest extends IntegrationTestBase {

    private static final String RANKING_PATH = "/api/v1/stats/teams/ranking";

    private Sport football;
    private Sport baseball;
    private FootballTeam tottenham;
    private FootballTeam liverpool;
    private BaseballTeam doosan;

    @BeforeEach
    void seed() {
        football = createSport("Football", "축구");
        baseball = createSport("Baseball", "야구");

        Continent europe = createContinent("Europe", "유럽", "EU");
        Continent asia = createContinent("Asia", "아시아", "AS");
        Country england = createCountry(europe, "England", "잉글랜드", "GB");
        Country korea = createCountry(asia, "Korea", "대한민국", "KR");

        FootballLeague epl = createFootballLeague(football, england, "Premier League", "프리미어리그");
        BaseballLeague kbo = createBaseballLeague(baseball, korea, "KBO", "KBO 리그");

        tottenham = createFootballTeam(epl, "Tottenham", "토트넘");
        liverpool = createFootballTeam(epl, "Liverpool", "리버풀");
        doosan = createBaseballTeam(kbo, "Doosan", "두산 베어스");

        // 응원자 시드 — Tottenham 3, Liverpool 1, Doosan 2
        User u1 = createUser("kc-1", "user1");
        User u2 = createUser("kc-2", "user2");
        User u3 = createUser("kc-3", "user3");

        linkUserTeam(u1, football, tottenham.getId());
        linkUserTeam(u2, football, tottenham.getId());
        linkUserTeam(u3, football, tottenham.getId());

        // 한 사용자가 여러 종목을 응원할 수 있음 — uk_user_teams_user_sport는 (user, sport) 단위 락
        User u4 = createUser("kc-4", "user4");
        linkUserTeam(u4, football, liverpool.getId());

        User u5 = createUser("kc-5", "user5");
        linkUserTeam(u5, baseball, doosan.getId());
        // u4도 야구팀 응원 (다른 종목)
        linkUserTeam(u4, baseball, doosan.getId());
    }

    @Nested
    @DisplayName("GET /api/v1/stats/teams/ranking — FOLLOWERS")
    class FollowersRanking {

        @Test
        @DisplayName("sport=ALL — Football·Baseball 통합 후 응원자 수 내림차순")
        void all_followersDescending() throws Exception {
            // 기대 순위: Tottenham(3) > Doosan(2) > Liverpool(1)
            mockMvc.perform(get(RANKING_PATH)
                            .param("metric", "FOLLOWERS")
                            .param("sport", "ALL")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0].rank").value(1))
                    .andExpect(jsonPath("$.data[0].team.name").value("토트넘"))
                    .andExpect(jsonPath("$.data[0].followerCount").value(3))
                    .andExpect(jsonPath("$.data[1].rank").value(2))
                    .andExpect(jsonPath("$.data[1].team.name").value("두산 베어스"))
                    .andExpect(jsonPath("$.data[1].followerCount").value(2))
                    .andExpect(jsonPath("$.data[2].rank").value(3))
                    .andExpect(jsonPath("$.data[2].team.name").value("리버풀"))
                    .andExpect(jsonPath("$.data[2].followerCount").value(1));
        }

        @Test
        @DisplayName("sport=축구 — Football 팀만 반환")
        void footballOnly_filtersBaseball() throws Exception {
            mockMvc.perform(get(RANKING_PATH)
                            .param("metric", "FOLLOWERS")
                            .param("sport", "축구")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].team.name").value("토트넘"))
                    .andExpect(jsonPath("$.data[1].team.name").value("리버풀"));
        }

        @Test
        @DisplayName("sport=야구 — Baseball 팀만 반환")
        void baseballOnly_filtersFootball() throws Exception {
            mockMvc.perform(get(RANKING_PATH)
                            .param("metric", "FOLLOWERS")
                            .param("sport", "야구")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].team.name").value("두산 베어스"));
        }

        @Test
        @DisplayName("size 파라미터 — Top N 만 반환")
        void size_limitsResults() throws Exception {
            mockMvc.perform(get(RANKING_PATH)
                            .param("metric", "FOLLOWERS")
                            .param("sport", "ALL")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].team.name").value("토트넘"));
        }

        @Test
        @DisplayName("size 0 또는 음수 — 컨트롤러 @Min(1) 검증으로 400")
        void size_belowMin_returns400() throws Exception {
            mockMvc.perform(get(RANKING_PATH)
                            .param("size", "0"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/stats/teams/ranking — 캐시")
    class Caching {

        @Test
        @DisplayName("같은 인자로 두 번 호출 시 두 번째는 응답 동일 — 사이에 응원자 추가해도 변하지 않음")
        void cachedResponse_doesNotReflectFreshChanges() throws Exception {
            mockMvc.perform(get(RANKING_PATH)
                            .param("metric", "FOLLOWERS")
                            .param("sport", "축구")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].followerCount").value(3));

            // 캐시된 결과 — Liverpool에 응원자를 폭발적으로 추가해도 노출 안 됨
            for (int i = 0; i < 100; i++) {
                User extra = createUser("kc-extra-" + i, "extra-" + i);
                linkUserTeam(extra, football, liverpool.getId());
            }

            mockMvc.perform(get(RANKING_PATH)
                            .param("metric", "FOLLOWERS")
                            .param("sport", "축구")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    // 여전히 Tottenham이 1위, Liverpool followerCount는 1로 캐시된 그대로
                    .andExpect(jsonPath("$.data[0].team.name").value("토트넘"))
                    .andExpect(jsonPath("$.data[1].followerCount").value(1));
        }
    }
}
