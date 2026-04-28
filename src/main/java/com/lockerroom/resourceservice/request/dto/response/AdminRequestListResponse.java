package com.lockerroom.resourceservice.request.dto.response;

import com.lockerroom.resourceservice.request.model.enums.RequestStatus;
import com.lockerroom.resourceservice.request.model.enums.RequestType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "[관리자] 사용자 요청 목록 행 응답. 요청자 정보와 사유 포함.")
public record AdminRequestListResponse(
        @Schema(description = "요청 ID", example = "77")
        Long id,

        @Schema(description = "요청자 닉네임", example = "야구사랑러")
        String userNickname,

        @Schema(description = "요청 유형")
        RequestType type,

        @Schema(description = "요청 대상 이름", example = "키움 히어로즈")
        String name,

        @Schema(description = "요청 사유", example = "키움 히어로즈가 누락되어 있어 추가 요청합니다.")
        String reason,

        @Schema(description = "처리 상태")
        RequestStatus status,

        @Schema(description = "요청 등록 일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt
) {
}
