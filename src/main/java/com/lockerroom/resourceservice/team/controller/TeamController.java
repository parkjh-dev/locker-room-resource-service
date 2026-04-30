package com.lockerroom.resourceservice.team.controller;

import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.team.dto.response.TeamDashboardResponse;
import com.lockerroom.resourceservice.team.service.TeamService;
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

@Tag(name = "팀", description = "팀 게시판 헤더용 정보 (익명 접근 가능)")
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "팀 대시보드",
            description = "팀 게시판 헤더용 통합 정보 (팀 프로필 + 시즌 + 다음 경기 + 최근 경기 + 순위). " +
                    "현재 다음 경기·최근 경기·순위는 stub (null/빈 배열) — Match·Standing 도메인 신설 후 채워질 예정. 익명 접근 가능.")
    @SecurityRequirements
    @GetMapping("/{teamId}/dashboard")
    public ResponseEntity<ApiResponse<TeamDashboardResponse>> getDashboard(
            @Parameter(description = "팀 ID", example = "101") @PathVariable Long teamId) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getDashboard(teamId)));
    }
}
