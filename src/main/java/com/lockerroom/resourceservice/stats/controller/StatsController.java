package com.lockerroom.resourceservice.stats.controller;

import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.stats.dto.request.TeamRankingMetric;
import com.lockerroom.resourceservice.stats.dto.response.TeamRankingResponse;
import com.lockerroom.resourceservice.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "통계", description = "팀 랭킹 등 공개 통계 (익명 접근 가능, Redis 캐시 1시간)")
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Validated
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "팀 랭킹",
            description = "응원자 수 또는 일평균 게시글 수 기준 Top N 팀 랭킹. 익명 접근 가능. " +
                    "농구·배구는 entity 부재로 빈 결과 반환.")
    @SecurityRequirements
    @GetMapping("/teams/ranking")
    public ResponseEntity<ApiResponse<List<TeamRankingResponse>>> getTeamRanking(
            @Parameter(description = "정렬 기준", example = "FOLLOWERS")
            @RequestParam(defaultValue = "FOLLOWERS") TeamRankingMetric metric,

            @Parameter(description = "종목 필터 (ALL/축구/야구/농구/배구)", example = "ALL")
            @RequestParam(defaultValue = "ALL") String sport,

            @Parameter(description = "반환 개수 (1~10)", example = "3")
            @RequestParam(defaultValue = "3") @Min(1) @Max(10) int size) {

        return ResponseEntity.ok(
                ApiResponse.success(statsService.getTeamRanking(metric, sport, size)));
    }
}
