package com.lockerroom.resourceservice.integration.support;

import com.lockerroom.resourceservice.board.model.entity.Board;
import com.lockerroom.resourceservice.board.model.enums.BoardType;
import com.lockerroom.resourceservice.board.repository.BoardRepository;
import com.lockerroom.resourceservice.common.model.enums.Role;
import com.lockerroom.resourceservice.file.service.FileService;
import com.lockerroom.resourceservice.sport.model.entity.BaseballLeague;
import com.lockerroom.resourceservice.sport.model.entity.BaseballTeam;
import com.lockerroom.resourceservice.sport.model.entity.Continent;
import com.lockerroom.resourceservice.sport.model.entity.Country;
import com.lockerroom.resourceservice.sport.model.entity.FootballLeague;
import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;
import com.lockerroom.resourceservice.sport.model.entity.Sport;
import com.lockerroom.resourceservice.sport.repository.BaseballLeagueRepository;
import com.lockerroom.resourceservice.sport.repository.BaseballTeamRepository;
import com.lockerroom.resourceservice.sport.repository.ContinentRepository;
import com.lockerroom.resourceservice.sport.repository.CountryRepository;
import com.lockerroom.resourceservice.sport.repository.FootballLeagueRepository;
import com.lockerroom.resourceservice.sport.repository.FootballTeamRepository;
import com.lockerroom.resourceservice.sport.repository.SportRepository;
import com.lockerroom.resourceservice.user.model.entity.User;
import com.lockerroom.resourceservice.user.model.entity.UserTeam;
import com.lockerroom.resourceservice.user.repository.UserRepository;
import com.lockerroom.resourceservice.user.repository.UserTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * 통합 테스트 공통 기반 클래스.
 *
 * <p>전제: Docker 데몬이 실행 중이어야 함. CI에서는 Testcontainers 호환 런타임 필요.
 *
 * <p>구성:
 * <ul>
 *   <li>MariaDB 컨테이너 — 실제 DB UNIQUE 제약·트랜잭션 동작 검증</li>
 *   <li>Redis 컨테이너 — 멱등성(@Idempotent) + @Cacheable 캐시 검증</li>
 *   <li>{@code @AutoConfigureMockMvc} — 컨트롤러 → 서비스 → 리포지토리 풀 스택</li>
 *   <li>JWT 디코더는 mock — {@code with(jwt())} postprocessor로 인증 우회</li>
 *   <li>FileService mock — S3 의존성 격리</li>
 * </ul>
 *
 * <p>각 테스트 사이에 {@link #cleanDatabase()}로 모든 테이블 truncate.
 * 캐시도 함께 비워서 leak 방지.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@Import(IntegrationTestBase.SecurityTestBeans.class)
public abstract class IntegrationTestBase {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(
            DockerImageName.parse("mariadb:11.4"))
            .withDatabaseName("locker_room_test")
            .withReuse(true);

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.datasource.driver-class-name", MARIADB::getDriverClassName);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected CacheManager cacheManager;
    @Autowired protected StringRedisTemplate redisTemplate;

    @Autowired protected UserRepository userRepository;
    @Autowired protected UserTeamRepository userTeamRepository;
    @Autowired protected SportRepository sportRepository;
    @Autowired protected ContinentRepository continentRepository;
    @Autowired protected CountryRepository countryRepository;
    @Autowired protected FootballLeagueRepository footballLeagueRepository;
    @Autowired protected FootballTeamRepository footballTeamRepository;
    @Autowired protected BaseballLeagueRepository baseballLeagueRepository;
    @Autowired protected BaseballTeamRepository baseballTeamRepository;
    @Autowired protected BoardRepository boardRepository;

    @MockitoBean protected FileService fileService;

    /**
     * 각 테스트 시작 전 모든 테이블을 truncate + 캐시 무효화.
     *
     * <p>vote idempotency 등 일부 테스트는 실제 commit이 필요해 트랜잭션 롤백 방식 대신
     * 명시적 truncate를 사용. FK 검사를 끈 상태로 INFORMATION_SCHEMA에서 테이블 목록을
     * 동적으로 조회해 일괄 처리 — 엔티티 추가/제거 시 자동 반영.
     */
    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        List<String> tables = jdbc.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'",
                String.class);
        for (String t : tables) {
            jdbc.execute("TRUNCATE TABLE `" + t + "`");
        }
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");

        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });

        // 멱등성 키(idempotency:*) 등 cache abstraction이 모르는 raw key 도 제거
        var conn = redisTemplate.getRequiredConnectionFactory().getConnection();
        try {
            conn.serverCommands().flushDb();
        } finally {
            conn.close();
        }
    }

    /* ────────── Fixture helpers ────────── */

    protected User createUser(String keycloakId, String nickname, Role role) {
        return userRepository.save(User.builder()
                .keycloakId(keycloakId)
                .email(nickname + "@test.local")
                .emailVerified(true)
                .nickname(nickname)
                .role(role)
                .build());
    }

    protected User createUser(String keycloakId, String nickname) {
        return createUser(keycloakId, nickname, Role.USER);
    }

    protected Sport createSport(String nameEn, String nameKo) {
        return sportRepository.save(Sport.builder()
                .nameEn(nameEn)
                .nameKo(nameKo)
                .isActive(true)
                .build());
    }

    protected Continent createContinent(String nameEn, String nameKo, String code) {
        return continentRepository.save(Continent.builder()
                .nameEn(nameEn)
                .nameKo(nameKo)
                .code(code)
                .build());
    }

    protected Country createCountry(Continent continent, String nameEn, String nameKo, String code) {
        return countryRepository.save(Country.builder()
                .continent(continent)
                .nameEn(nameEn)
                .nameKo(nameKo)
                .code(code)
                .build());
    }

    protected FootballLeague createFootballLeague(Sport sport, Country country, String nameEn, String nameKo) {
        return footballLeagueRepository.save(FootballLeague.builder()
                .sport(sport)
                .country(country)
                .nameEn(nameEn)
                .nameKo(nameKo)
                .build());
    }

    protected FootballTeam createFootballTeam(FootballLeague league, String nameEn, String nameKo) {
        return footballTeamRepository.save(FootballTeam.builder()
                .league(league)
                .nameEn(nameEn)
                .nameKo(nameKo)
                .build());
    }

    protected BaseballLeague createBaseballLeague(Sport sport, Country country, String nameEn, String nameKo) {
        return baseballLeagueRepository.save(BaseballLeague.builder()
                .sport(sport)
                .country(country)
                .nameEn(nameEn)
                .nameKo(nameKo)
                .build());
    }

    protected BaseballTeam createBaseballTeam(BaseballLeague league, String nameEn, String nameKo) {
        return baseballTeamRepository.save(BaseballTeam.builder()
                .league(league)
                .nameEn(nameEn)
                .nameKo(nameKo)
                .build());
    }

    protected Board createBoard(String name, BoardType type) {
        return boardRepository.save(Board.builder()
                .name(name)
                .type(type)
                .build());
    }

    protected UserTeam linkUserTeam(User user, Sport sport, Long teamId) {
        return userTeamRepository.save(UserTeam.builder()
                .user(user)
                .sport(sport)
                .teamId(teamId)
                .build());
    }

    /* ────────── Security helpers ────────── */

    /**
     * jwt() postprocessor로 인증된 요청 만들기. CurrentUserIdArgumentResolver는
     * principal에서 subject를 꺼내 keycloakId로 사용자 조회 → DB의 user.id를 주입한다.
     */
    protected RequestPostProcessor jwtFor(User user) {
        String roleAuthority = "ROLE_" + user.getRole().name();
        return jwt()
                .jwt(j -> j.subject(user.getKeycloakId()))
                .authorities(() -> roleAuthority);
    }

    /**
     * 익명 요청 동등 효과 — JWT를 셋하지 않고 보낼 때 사용. 명시적 readability를 위해 헬퍼만 제공.
     */
    protected RequestPostProcessor anonymous() {
        return request -> request;
    }

    /* ────────── Test wiring beans ────────── */

    /**
     * JWT 디코더 mock — 실제 IDP fetch 차단. {@code with(jwt())}가 SecurityContext를
     * 직접 셋하므로 디코더는 실제 호출되지 않지만, 부팅 시 빈 생성을 만족시키기 위해 필요.
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class SecurityTestBeans {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public JwtDecoder jwtDecoder() {
            return token -> {
                throw new IllegalStateException(
                        "JwtDecoder는 통합 테스트에서 호출되어선 안 됨 — with(jwt())로 우회하세요.");
            };
        }
    }
}
