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
import com.lockerroom.resourceservice.stats.dto.response.TeamSummaryResponse;
import com.lockerroom.resourceservice.stats.service.StatsService;
import com.lockerroom.resourceservice.user.repository.UserTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private static final String SPORT_FOOTBALL = "Football";
    private static final String SPORT_BASEBALL = "Baseball";
    private static final String FILTER_ALL = "ALL";
    /** 결정 #6 — avgPostsPerDay 계산 기간. */
    private static final int RANKING_DAYS_WINDOW = 30;

    private final SportRepository sportRepository;
    private final FootballTeamRepository footballTeamRepository;
    private final BaseballTeamRepository baseballTeamRepository;
    private final FootballPostRepository footballPostRepository;
    private final BaseballPostRepository baseballPostRepository;
    private final UserTeamRepository userTeamRepository;

    /**
     * 결정 #11 — 캐시 TTL 1시간 (RedisConfig 기본값). 캐시 키는 metric+sport+size.
     */
    @Override
    @Cacheable(value = "teamRanking",
            key = "#metric.name() + ':' + #sport + ':' + #size")
    public List<TeamRankingResponse> getTeamRanking(TeamRankingMetric metric, String sport, int size) {
        boolean includeFootball = FILTER_ALL.equals(sport) || "축구".equals(sport);
        boolean includeBaseball = FILTER_ALL.equals(sport) || "야구".equals(sport);

        List<RankingRow> rows = new ArrayList<>();
        if (includeFootball) {
            rows.addAll(buildFootballRows());
        }
        if (includeBaseball) {
            rows.addAll(buildBaseballRows());
        }
        // 농구·배구는 entity 부재 — 빈 결과 추가 안 함

        Comparator<RankingRow> comparator = (metric == TeamRankingMetric.FOLLOWERS)
                ? Comparator.comparingLong(RankingRow::followerCount).reversed()
                : Comparator.comparingInt(RankingRow::avgPostsPerDay).reversed();

        List<RankingRow> sorted = rows.stream()
                .sorted(comparator)
                .limit(Math.max(1, size))
                .toList();

        List<TeamRankingResponse> result = new ArrayList<>();
        int rank = 1;
        for (RankingRow r : sorted) {
            result.add(new TeamRankingResponse(
                    rank++,
                    new TeamSummaryResponse(r.teamId(), r.teamName(), r.logoUrl()),
                    r.sportName(),
                    r.followerCount(),
                    r.avgPostsPerDay()
            ));
        }
        return result;
    }

    /* ────────── Football ────────── */

    private List<RankingRow> buildFootballRows() {
        Sport sport = sportRepository.findByNameEnIgnoreCase(SPORT_FOOTBALL).orElse(null);
        if (sport == null) return List.of();

        Map<Long, Long> followerByTeam = followerCountsBySport(sport.getId());
        Map<Long, Integer> avgByTeam = footballPostCountsByTeam();

        List<FootballTeam> teams = footballTeamRepository.findAll();
        List<RankingRow> rows = new ArrayList<>(teams.size());
        for (FootballTeam team : teams) {
            rows.add(new RankingRow(
                    team.getId(),
                    team.getNameKo(),
                    team.getLogoUrl(),
                    sport.getNameKo(),
                    followerByTeam.getOrDefault(team.getId(), 0L),
                    avgByTeam.getOrDefault(team.getId(), 0)
            ));
        }
        return rows;
    }

    private Map<Long, Integer> footballPostCountsByTeam() {
        LocalDateTime since = LocalDateTime.now().minusDays(RANKING_DAYS_WINDOW);
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : footballPostRepository.countByTeamSince(since)) {
            Long teamId = (Long) row[0];
            long count = ((Number) row[1]).longValue();
            result.put(teamId, (int) Math.round((double) count / RANKING_DAYS_WINDOW));
        }
        return result;
    }

    /* ────────── Baseball ────────── */

    private List<RankingRow> buildBaseballRows() {
        Sport sport = sportRepository.findByNameEnIgnoreCase(SPORT_BASEBALL).orElse(null);
        if (sport == null) return List.of();

        Map<Long, Long> followerByTeam = followerCountsBySport(sport.getId());
        Map<Long, Integer> avgByTeam = baseballPostCountsByTeam();

        List<BaseballTeam> teams = baseballTeamRepository.findAll();
        List<RankingRow> rows = new ArrayList<>(teams.size());
        for (BaseballTeam team : teams) {
            rows.add(new RankingRow(
                    team.getId(),
                    team.getNameKo(),
                    team.getLogoUrl(),
                    sport.getNameKo(),
                    followerByTeam.getOrDefault(team.getId(), 0L),
                    avgByTeam.getOrDefault(team.getId(), 0)
            ));
        }
        return rows;
    }

    private Map<Long, Integer> baseballPostCountsByTeam() {
        LocalDateTime since = LocalDateTime.now().minusDays(RANKING_DAYS_WINDOW);
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : baseballPostRepository.countByTeamSince(since)) {
            Long teamId = (Long) row[0];
            long count = ((Number) row[1]).longValue();
            result.put(teamId, (int) Math.round((double) count / RANKING_DAYS_WINDOW));
        }
        return result;
    }

    /** 종목 내 모든 팀의 응원자 수를 단일 GROUP BY 쿼리로 집계 (N+1 회피). */
    private Map<Long, Long> followerCountsBySport(Long sportId) {
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : userTeamRepository.countFollowersBySport(sportId)) {
            Long teamId = (Long) row[0];
            long count = ((Number) row[1]).longValue();
            result.put(teamId, count);
        }
        return result;
    }

    /* ────────── 내부 통합 row ────────── */

    private record RankingRow(
            Long teamId,
            String teamName,
            String logoUrl,
            String sportName,
            long followerCount,
            int avgPostsPerDay
    ) {
    }
}
