package com.lockerroom.resourceservice.request.dto.response;

import com.lockerroom.resourceservice.request.model.enums.RequestStatus;
import com.lockerroom.resourceservice.request.model.enums.RequestType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "내 요청 목록 행 응답.")
public record RequestListResponse(
        @Schema(description = "요청 ID", example = "77")
        Long id,

        @Schema(description = "요청 유형")
        RequestType type,

        @Schema(description = "요청 대상 이름", example = "키움 히어로즈")
        String name,

        @Schema(description = "처리 상태")
        RequestStatus status,

        @Schema(description = "요청 등록 일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt
) {
}
