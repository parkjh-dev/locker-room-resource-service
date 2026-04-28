package com.lockerroom.resourceservice.request.dto.request;

import com.lockerroom.resourceservice.request.model.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "[관리자] 요청 처리 (승인/반려). status에 따라 추가 필드 의미가 다름.")
public record RequestProcessRequest(
        @Schema(description = "처리 결과 상태 (APPROVED, REJECTED)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull RequestStatus status,

        @Schema(description = "반려 사유 (status=REJECTED일 때 필수)", example = "이미 등록된 팀입니다.", nullable = true)
        String rejectReason,

        @Schema(description = "승인 시 등록할 종목 ID (TEAM/LEAGUE 요청 승인용)", example = "2", nullable = true)
        Long sportId,

        @Schema(description = "승인 시 소속 리그 ID (TEAM 요청 승인용)", example = "10", nullable = true)
        Long leagueId
) {
}
