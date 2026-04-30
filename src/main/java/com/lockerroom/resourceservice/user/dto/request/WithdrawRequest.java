package com.lockerroom.resourceservice.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원 탈퇴 요청. 사유는 통계 목적으로 기록되며 비밀번호는 본인 확인용.")
public record WithdrawRequest(
        @Schema(description = "탈퇴 사유 (자유 텍스트, 통계용)", example = "사용 빈도가 줄어서", nullable = true)
        String reason,

        @Schema(description = "본인 확인용 현재 비밀번호. 필수.", example = "currentPw1234!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String password
) {
}
