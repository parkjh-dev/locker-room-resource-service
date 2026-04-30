package com.lockerroom.resourceservice.sport.controller;

import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.sport.dto.response.TeamResponse;
import com.lockerroom.resourceservice.sport.service.LeagueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "리그", description = "리그 단위 조회 (익명 접근 가능)")
@RestController
@RequestMapping("/api/v1/leagues")
@RequiredArgsConstructor
public class LeagueController {

    private final LeagueService leagueService;

    @Operation(summary = "리그별 팀 목록",
            description = "지정한 리그에 소속된 팀 목록을 반환합니다. 축구·야구 리그 모두 지원. 익명 접근 가능.")
    @SecurityRequirements
    @GetMapping("/{leagueId}/teams")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsByLeague(
            @Parameter(description = "리그 ID", example = "1") @PathVariable Long leagueId) {
        return ResponseEntity.ok(ApiResponse.success(leagueService.getTeamsByLeague(leagueId)));
    }
}
