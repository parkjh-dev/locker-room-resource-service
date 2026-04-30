package com.lockerroom.resourceservice.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 팀 게시판 헤더용 통합 응답 — round-trip 1번에 팀 정보·다음 경기·최근 경기·순위 노출.
 *
 * <p><b>현재 상태 (TODO)</b>: Match·Standing 도메인이 아직 신설되지 않아 다음 항목은 stub:
 * <ul>
 *   <li>{@code nextMatch} — null</li>
 *   <li>{@code recentMatches} — 빈 배열</li>
 *   <li>{@code standing} — null</li>
 * </ul>
 * <p>외부 스포츠 데이터 API(K리그/KBO 공식 OPI 등) 연동 또는 운영 어드민 입력 채널 확정 후
 * 별도 phase에서 채워질 예정.
 */
@Schema(description = "팀 게시판 헤더용 통합 대시보드.")
public record TeamDashboardResponse(
        @Schema(description = "팀 프로필")
        TeamProfileResponse team,

        @Schema(description = "시즌 라벨 (종목·리그별 자유 포맷)", example = "2026")
        String season,

        @Schema(description = "다음 경기. 예정된 경기 없으면 null.", nullable = true)
        UpcomingMatchResponse nextMatch,

        @Schema(description = "최근 경기 (최신 → 과거 순, 최대 5개)")
        List<RecentMatchResponse> recentMatches,

        @Schema(description = "현재 시즌 순위. 시즌 시작 전·시즌 외엔 null.", nullable = true)
        TeamStandingResponse standing
) {
}
