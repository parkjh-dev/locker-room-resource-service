package com.lockerroom.resourceservice.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

@Schema(description = "[관리자] 사용자 정지 요청. 사유와 정지 만료 시점 필수.")
public record SuspendRequest(
        @Schema(description = "정지 사유 (사용자에게 노출, 최대 500자)", example = "반복적인 욕설 사용", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 500) String reason,

        @Schema(description = "정지 만료 시점 (ISO-8601, 미래여야 함)", example = "2026-05-28T00:00:00+09:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Future OffsetDateTime suspendedUntil
) {
}
