package com.lockerroom.resourceservice.integration;

import com.lockerroom.resourceservice.integration.support.IntegrationTestBase;
import com.lockerroom.resourceservice.sport.model.entity.BaseballLeague;
import com.lockerroom.resourceservice.sport.model.entity.BaseballTeam;
import com.lockerroom.resourceservice.sport.model.entity.Continent;
import com.lockerroom.resourceservice.sport.model.entity.Country;
import com.lockerroom.resourceservice.sport.model.entity.FootballLeague;
import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;
import com.lockerroom.resourceservice.sport.model.entity.Sport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 스포츠 cascading 조회 통합 테스트.
 *
 * <p>핵심 검증:
 * <ul>
 *   <li>익명 접근 가능 — JWT 없이 200</li>
 *   <li>switch 분기 — Football/Baseball/기타 sport 별로 다른 repository 호출</li>
 *   <li>한국(KR) 우선 정렬 — 다국가 응답에서 KR이 최상단</li>
 *   <li>Football sport에 등록된 country만 반환 — 야구만 운영하는 국가는 제외</li>
 * </ul>
 */
class SportCascadingIntegrationTest extends IntegrationTestBase {

    private Sport football;
    private Sport baseball;
    private Sport basketball; // 미지원 종목
    private Country korea;
    private Country england;
    private Country usa;
    private FootballLeague kLeague;
    private FootballLeague epl;
    private BaseballLeague kbo;

    @BeforeEach
    void seed() {
        football = createSport("Football", "축구");
        baseball = createSport("Baseball", "야구");
        basketball = createSport("Basketball", "농구");

        Continent europe = createContinent("Europe", "유럽", "EU");
        Continent asia = createContinent("Asia", "아시아", "AS");
        Continent america = createContinent("America", "아메리카", "AM");

        korea = createCountry(asia, "Korea", "대한민국", "KR");
        england = createCountry(europe, "England", "잉글랜드", "GB");
        usa = createCountry(america, "USA", "미국", "US"); // 야구만 운영

        kLeague = createFootballLeague(football, korea, "K League 1", "K리그1");
        epl = createFootballLeague(football, england, "Premier League", "프리미어리그");
        kbo = createBaseballLeague(baseball, korea, "KBO", "KBO 리그");
        createBaseballLeague(baseball, usa, "MLB", "메이저리그"); // 미국은 야구만

        createFootballTeam(kLeague, "Jeonbuk Hyundai", "전북 현대");
        createFootballTeam(epl, "Tottenham", "토트넘");
    }

    @Nested
    @DisplayName("GET /api/v1/sports")
    class GetSports {

        @Test
        @DisplayName("익명 접근 — 활성 sport 만 반환")
        void getSports_anonymous_returnsActive() throws Exception {
            mockMvc.perform(get("/api/v1/sports"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[?(@.nameEn == 'Football')]").isNotEmpty())
                    .andExpect(jsonPath("$.data[?(@.nameEn == 'Baseball')]").isNotEmpty())
                    .andExpect(jsonPath("$.data[?(@.nameEn == 'Basketball')]").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sports/{id}/countries")
    class GetCountriesBySport {

        @Test
        @DisplayName("Football: 한국·잉글랜드만 반환, KR 최상단")
        void football_returnsCountriesWithKoreaFirst() throws Exception {
            mockMvc.perform(get("/api/v1/sports/{id}/countries", football.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].code").value("KR"))
                    .andExpect(jsonPath("$.data[1].code").value("GB"));
        }

        @Test
        @DisplayName("Baseball: 한국·미국 모두 반환 (KR 최상단)")
        void baseball_returnsKoreaAndUsa() throws Exception {
            mockMvc.perform(get("/api/v1/sports/{id}/countries", baseball.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].code").value("KR"));
        }

        @Test
        @DisplayName("Basketball: 미지원 종목 — 빈 배열")
        void basketball_returnsEmpty() throws Exception {
            mockMvc.perform(get("/api/v1/sports/{id}/countries", basketball.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("존재하지 않는 sport ID — 404 SPORT_NOT_FOUND")
        void unknownSportId_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/sports/{id}/countries", 9_999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SPORT_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/sports/{sportId}/countries/{countryId}/leagues")
    class GetLeaguesBySportAndCountry {

        @Test
        @DisplayName("Football + Korea — K리그만 반환 (Baseball KBO는 제외)")
        void football_korea_returnsKLeagueOnly() throws Exception {
            mockMvc.perform(get("/api/v1/sports/{s}/countries/{c}/leagues",
                            football.getId(), korea.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(kLeague.getId()))
                    .andExpect(jsonPath("$.data[0].nameKo").value("K리그1"));
        }

        @Test
        @DisplayName("Baseball + Korea — KBO만 반환")
        void baseball_korea_returnsKboOnly() throws Exception {
            mockMvc.perform(get("/api/v1/sports/{s}/countries/{c}/leagues",
                            baseball.getId(), korea.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(kbo.getId()))
                    .andExpect(jsonPath("$.data[0].nameKo").value("KBO 리그"));
        }

        @Test
        @DisplayName("Basketball — 미지원 종목, 빈 배열")
        void basketball_anyCountry_returnsEmpty() throws Exception {
            mockMvc.perform(get("/api/v1/sports/{s}/countries/{c}/leagues",
                            basketball.getId(), korea.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/leagues/{id}/teams")
    class GetTeamsByLeague {

        @Test
        @DisplayName("Football 리그 ID — Football 팀 목록")
        void footballLeague_returnsFootballTeams() throws Exception {
            mockMvc.perform(get("/api/v1/leagues/{id}/teams", epl.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("토트넘"));
        }

        @Test
        @DisplayName("Baseball 리그 ID — fallback 분기로 Baseball 팀 목록")
        void baseballLeague_returnsBaseballTeams() throws Exception {
            BaseballTeam doosan = createBaseballTeam(kbo, "Doosan", "두산 베어스");

            mockMvc.perform(get("/api/v1/leagues/{id}/teams", kbo.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("두산 베어스"))
                    .andExpect(jsonPath("$.data[0].id").value(doosan.getId()));
        }

        @Test
        @DisplayName("존재하지 않는 리그 ID — 404 LEAGUE_NOT_FOUND")
        void unknownLeague_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/leagues/{id}/teams", 9_999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("LEAGUE_NOT_FOUND"));
        }
    }
}
