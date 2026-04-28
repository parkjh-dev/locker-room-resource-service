package com.lockerroom.resourceservice.request.dto.request;

import com.lockerroom.resourceservice.request.model.enums.RequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 요청 등록 (신규 팀/리그 등록 등).")
public record RequestCreateRequest(
        @Schema(description = "요청 유형 (TEAM, LEAGUE 등)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull RequestType type,

        @Schema(description = "요청하는 대상 이름 (팀명/리그명, 최대 100자)", example = "키움 히어로즈", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100) String name,

        @Schema(description = "요청 사유 (관리자 검토용, 최대 2000자)", example = "키움 히어로즈가 누락되어 있어 추가 요청합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 2000) String reason
) {
}
