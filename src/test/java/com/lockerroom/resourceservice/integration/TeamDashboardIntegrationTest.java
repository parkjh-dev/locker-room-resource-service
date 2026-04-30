package com.lockerroom.resourceservice.integration;

import com.lockerroom.resourceservice.integration.support.IntegrationTestBase;
import com.lockerroom.resourceservice.sport.model.entity.BaseballLeague;
import com.lockerroom.resourceservice.sport.model.entity.BaseballTeam;
import com.lockerroom.resourceservice.sport.model.entity.Continent;
import com.lockerroom.resourceservice.sport.model.entity.Country;
import com.lockerroom.resourceservice.sport.model.entity.FootballLeague;
import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;
import com.lockerroom.resourceservice.sport.model.entity.Sport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 팀 대시보드 통합 테스트.
 *
 * <p>현재는 stub 상태 (Match·Standing 도메인 미구현). 검증 포인트:
 * <ul>
 *   <li>Football/Baseball 모두에서 매칭이 성공해야 함 — fallback 분기</li>
 *   <li>존재하지 않는 팀 ID — 404 RESOURCE_NOT_FOUND</li>
 *   <li>nextMatch / standing 은 null, recentMatches 는 빈 배열</li>
 * </ul>
 */
class TeamDashboardIntegrationTest extends IntegrationTestBase {

    private static final String DASHBOARD_PATH = "/api/v1/teams/{teamId}/dashboard";

    @Nested
    @DisplayName("GET /api/v1/teams/{teamId}/dashboard")
    class GetDashboard {

        @Test
        @DisplayName("Football 팀 — 프로필·시즌 반환, stub 필드는 null/빈 배열")
        void football_team_returnsProfileAndStubs() throws Exception {
            Sport football = createSport("Football", "축구");
            Continent europe = createContinent("Europe", "유럽", "EU");
            Country england = createCountry(europe, "England", "잉글랜드", "GB");
            FootballLeague epl = createFootballLeague(football, england, "Premier League", "프리미어리그");
            FootballTeam tottenham = createFootballTeam(epl, "Tottenham", "토트넘");

            mockMvc.perform(get(DASHBOARD_PATH, tottenham.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.team.id").value(tottenham.getId()))
                    .andExpect(jsonPath("$.data.team.name").value("토트넘"))
                    .andExpect(jsonPath("$.data.team.leagueName").value("프리미어리그"))
                    .andExpect(jsonPath("$.data.season").value("2026"))
                    .andExpect(jsonPath("$.data.nextMatch").doesNotExist())
                    .andExpect(jsonPath("$.data.standing").doesNotExist())
                    .andExpect(jsonPath("$.data.recentMatches.length()").value(0));
        }

        @Test
        @DisplayName("Baseball 팀 — Football fallback 후 Baseball repository 매칭")
        void baseball_team_resolvesViaFallback() throws Exception {
            Sport baseball = createSport("Baseball", "야구");
            Continent asia = createContinent("Asia", "아시아", "AS");
            Country korea = createCountry(asia, "Korea", "대한민국", "KR");
            BaseballLeague kbo = createBaseballLeague(baseball, korea, "KBO", "KBO 리그");
            BaseballTeam doosan = createBaseballTeam(kbo, "Doosan", "두산 베어스");

            mockMvc.perform(get(DASHBOARD_PATH, doosan.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.team.id").value(doosan.getId()))
                    .andExpect(jsonPath("$.data.team.name").value("두산 베어스"))
                    .andExpect(jsonPath("$.data.team.leagueName").value("KBO 리그"));
        }

        @Test
        @DisplayName("Football·Baseball 어느 쪽에도 없는 팀 ID — 404 COMMON_NOT_FOUND")
        void unknownTeam_returns404() throws Exception {
            mockMvc.perform(get(DASHBOARD_PATH, 99_999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"));
        }

        @Test
        @DisplayName("익명 접근 가능 — JWT 없이 200")
        void anonymous_canAccess() throws Exception {
            Sport football = createSport("Football", "축구");
            Continent europe = createContinent("Europe", "유럽", "EU");
            Country england = createCountry(europe, "England", "잉글랜드", "GB");
            FootballLeague epl = createFootballLeague(football, england, "EPL", "프리미어리그");
            FootballTeam team = createFootballTeam(epl, "Arsenal", "아스널");

            // jwtFor 없이 호출 — SecurityConfig가 GET /api/v1/teams/** permitAll 임을 확인
            mockMvc.perform(get(DASHBOARD_PATH, team.getId()))
                    .andExpect(status().isOk());
        }
    }
}
