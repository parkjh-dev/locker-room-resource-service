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
import com.lockerroom.resourceservice.user.model.entity.UserTeam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 사용자 도메인 통합 테스트.
 *
 * <p>핵심 시나리오:
 * <ul>
 *   <li>/me — JWT subject(keycloakId) → DB user.id 매핑이 풀 스택에서 동작하는지</li>
 *   <li>응원팀 등록 — 종목별 락(uk_user_teams_user_sport) DB 제약이 실제 충돌 시점에 강제되는지</li>
 *   <li>sport-team 매칭 검증 — Football sport에 BaseballTeam.id를 보내면 INVALID_TEAM_FOR_SPORT</li>
 *   <li>온보딩 skip 멱등성 — 두 번 호출해도 onboardingCompletedAt 시각이 첫 호출로 고정</li>
 * </ul>
 */
class UserIntegrationTest extends IntegrationTestBase {

    private Sport football;
    private Sport baseball;
    private FootballTeam tottenham;
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
        doosan = createBaseballTeam(kbo, "Doosan Bears", "두산 베어스");
    }

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetMyInfo {

        @Test
        @DisplayName("JWT subject로 DB 사용자를 조회해 프로필을 반환한다")
        void me_returnsUserProfile() throws Exception {
            User user = createUser("kc-user-1", "tester");

            mockMvc.perform(get("/api/v1/users/me").with(jwtFor(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(user.getId()))
                    .andExpect(jsonPath("$.data.nickname").value("tester"))
                    .andExpect(jsonPath("$.data.onboardingCompletedAt").doesNotExist());
        }

        @Test
        @DisplayName("DB에 사용자가 없으면 404 USER_NOT_FOUND")
        void me_unknownKeycloakId_returns404() throws Exception {
            // 미가입 사용자: JWT subject만 갖고 들어오는 케이스 (예: Keycloak에서 가입했으나 동기화 전)
            User ghost = User.builder()
                    .keycloakId("kc-ghost")
                    .email("ghost@test.local")
                    .nickname("ghost")
                    .build();
            // userRepository.save 안 함 — DB에 없는 상태로 jwt만 만들어 호출

            mockMvc.perform(get("/api/v1/users/me").with(jwtFor(ghost)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/me/teams (응원팀 등록)")
    class AddUserTeams {

        @Test
        @DisplayName("등록 성공 시 onboardingCompletedAt 자동 셋, teams 리스트 반영")
        void addTeams_success() throws Exception {
            User user = createUser("kc-user-1", "tester");

            String body = objectMapper.writeValueAsString(Map.of(
                    "teams", List.of(
                            Map.of("sportId", football.getId(), "teamId", tottenham.getId()))));

            mockMvc.perform(post("/api/v1/users/me/teams")
                            .with(jwtFor(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.onboardingCompletedAt").isNotEmpty())
                    .andExpect(jsonPath("$.data.teams.length()").value(1))
                    .andExpect(jsonPath("$.data.teams[0].teamId").value(tottenham.getId()));

            List<UserTeam> saved = userTeamRepository.findByUserId(user.getId());
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getSport().getId()).isEqualTo(football.getId());
            assertThat(saved.get(0).getTeamId()).isEqualTo(tottenham.getId());
        }

        @Test
        @DisplayName("이미 등록한 종목으로 재요청 시 409 — 사전 체크가 트랜잭션 진입 직후 차단")
        void addTeams_duplicateSport_returns409() throws Exception {
            User user = createUser("kc-user-1", "tester");
            linkUserTeam(user, football, tottenham.getId());

            FootballTeam liverpool = createFootballTeam(
                    tottenham.getLeague(), "Liverpool", "리버풀");

            String body = objectMapper.writeValueAsString(Map.of(
                    "teams", List.of(
                            Map.of("sportId", football.getId(), "teamId", liverpool.getId()))));

            mockMvc.perform(post("/api/v1/users/me/teams")
                            .with(jwtFor(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("USER_TEAM_ALREADY_REGISTERED"));

            // Football 등록은 그대로 유지(toetnham), Liverpool로 변경되지 않았어야 함
            List<UserTeam> saved = userTeamRepository.findByUserId(user.getId());
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getTeamId()).isEqualTo(tottenham.getId());
        }

        @Test
        @DisplayName("Football sport에 Baseball 팀 ID를 보내면 400 INVALID_TEAM_FOR_SPORT")
        void addTeams_mismatchSportTeam_returns400() throws Exception {
            User user = createUser("kc-user-1", "tester");

            String body = objectMapper.writeValueAsString(Map.of(
                    "teams", List.of(
                            Map.of("sportId", football.getId(), "teamId", doosan.getId()))));

            mockMvc.perform(post("/api/v1/users/me/teams")
                            .with(jwtFor(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("USER_TEAM_INVALID_FOR_SPORT"));

            // 트랜잭션 롤백 검증 — 아무것도 저장되지 않음
            assertThat(userTeamRepository.findByUserId(user.getId())).isEmpty();
        }

        @Test
        @DisplayName("여러 종목 등록 중 두 번째 종목이 중복이면 첫 번째까지 모두 롤백")
        void addTeams_partialDuplicate_rollsBackAll() throws Exception {
            User user = createUser("kc-user-1", "tester");
            // 미리 Baseball 등록
            linkUserTeam(user, baseball, doosan.getId());

            // Football(신규) + Baseball(중복) 같이 보내면 전체 롤백
            String body = objectMapper.writeValueAsString(Map.of(
                    "teams", List.of(
                            Map.of("sportId", football.getId(), "teamId", tottenham.getId()),
                            Map.of("sportId", baseball.getId(), "teamId", doosan.getId()))));

            mockMvc.perform(post("/api/v1/users/me/teams")
                            .with(jwtFor(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());

            // Football 신규 등록이 롤백되어 baseball 1건만 남아야 함
            List<UserTeam> saved = userTeamRepository.findByUserId(user.getId());
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getSport().getId()).isEqualTo(baseball.getId());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/me/onboarding/skip")
    class SkipOnboarding {

        @Test
        @DisplayName("최초 호출 시 onboardingCompletedAt 셋")
        void skip_firstCall_setsTimestamp() throws Exception {
            User user = createUser("kc-user-1", "tester");
            assertThat(user.getOnboardingCompletedAt()).isNull();

            mockMvc.perform(post("/api/v1/users/me/onboarding/skip").with(jwtFor(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.onboardingCompletedAt").isNotEmpty());

            User refreshed = userRepository.findById(user.getId()).orElseThrow();
            assertThat(refreshed.getOnboardingCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("두 번 호출해도 시각은 첫 호출 값으로 고정 (idempotent)")
        void skip_idempotent_doesNotOverwriteTimestamp() throws Exception {
            User user = createUser("kc-user-1", "tester");

            mockMvc.perform(post("/api/v1/users/me/onboarding/skip").with(jwtFor(user)))
                    .andExpect(status().isOk());

            var firstTimestamp = userRepository.findById(user.getId())
                    .orElseThrow().getOnboardingCompletedAt();
            assertThat(firstTimestamp).isNotNull();

            // 시각 차이를 보장하기 위해 잠시 대기
            Thread.sleep(50);

            mockMvc.perform(post("/api/v1/users/me/onboarding/skip").with(jwtFor(user)))
                    .andExpect(status().isOk());

            var secondTimestamp = userRepository.findById(user.getId())
                    .orElseThrow().getOnboardingCompletedAt();
            assertThat(secondTimestamp).isEqualTo(firstTimestamp);
        }
    }
}
