package com.lockerroom.resourceservice.request.dto.response;

import com.lockerroom.resourceservice.request.model.enums.RequestStatus;
import com.lockerroom.resourceservice.request.model.enums.RequestType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사용자 요청 상세 응답. 처리 결과 포함.")
public record RequestDetailResponse(
        @Schema(description = "요청 ID", example = "77")
        Long id,

        @Schema(description = "요청 유형")
        RequestType type,

        @Schema(description = "요청 대상 이름", example = "키움 히어로즈")
        String name,

        @Schema(description = "요청 사유", example = "키움 히어로즈가 누락되어 있어 추가 요청합니다.")
        String reason,

        @Schema(description = "처리 상태 (PENDING, APPROVED, REJECTED)")
        RequestStatus status,

        @Schema(description = "반려 사유 (status=REJECTED일 때만)", example = "이미 등록된 팀입니다.", nullable = true)
        String rejectReason,

        @Schema(description = "처리 일시 (status=PENDING이면 null)", example = "2026-04-28T11:00:00", nullable = true)
        LocalDateTime processedAt,

        @Schema(description = "요청 등록 일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt
) {
}
