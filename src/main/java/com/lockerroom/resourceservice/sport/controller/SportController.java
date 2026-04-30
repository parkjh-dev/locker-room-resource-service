package com.lockerroom.resourceservice.sport.controller;

import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.sport.dto.response.CountryResponse;
import com.lockerroom.resourceservice.sport.dto.response.LeagueResponse;
import com.lockerroom.resourceservice.sport.dto.response.SportResponse;
import com.lockerroom.resourceservice.sport.service.SportService;
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

@Tag(name = "스포츠", description = "스포츠 종목·국가·리그 cascading 조회 (익명 접근 가능)")
@RestController
@RequestMapping("/api/v1/sports")
@RequiredArgsConstructor
public class SportController {

    private final SportService sportService;

    @Operation(summary = "스포츠 종목 목록", description = "활성화된 스포츠 종목(축구, 야구 등) 목록을 반환합니다. 익명 접근 가능.")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<List<SportResponse>>> getSports() {
        return ResponseEntity.ok(ApiResponse.success(sportService.getSports()));
    }

    @Operation(summary = "종목별 국가 목록",
            description = "해당 종목의 리그가 등록된 국가만 반환. 한국(KR)이 최상단에 정렬됩니다. 익명 접근 가능.")
    @SecurityRequirements
    @GetMapping("/{sportId}/countries")
    public ResponseEntity<ApiResponse<List<CountryResponse>>> getCountriesBySport(
            @Parameter(description = "종목 ID", example = "1") @PathVariable Long sportId) {
        return ResponseEntity.ok(ApiResponse.success(sportService.getCountriesBySport(sportId)));
    }

    @Operation(summary = "종목·국가별 리그 목록",
            description = "선택한 종목·국가 조합으로 운영되는 리그 목록을 반환합니다. 익명 접근 가능.")
    @SecurityRequirements
    @GetMapping("/{sportId}/countries/{countryId}/leagues")
    public ResponseEntity<ApiResponse<List<LeagueResponse>>> getLeaguesByCountryAndSport(
            @Parameter(description = "종목 ID", example = "1") @PathVariable Long sportId,
            @Parameter(description = "국가 ID", example = "1") @PathVariable Long countryId) {
        return ResponseEntity.ok(
                ApiResponse.success(sportService.getLeaguesByCountryAndSport(sportId, countryId)));
    }
}
