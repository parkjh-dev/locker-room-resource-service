package com.lockerroom.resourceservice.user.dto.response;

import com.lockerroom.resourceservice.common.model.enums.Role;
import com.lockerroom.resourceservice.user.model.enums.OAuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "[관리자] 사용자 목록 행 응답. 정지 여부 포함.")
public record AdminUserListResponse(
        @Schema(description = "사용자 ID", example = "42")
        Long id,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "닉네임", example = "야구사랑러")
        String nickname,

        @Schema(description = "권한")
        Role role,

        @Schema(description = "OAuth 제공자")
        OAuthProvider provider,

        @Schema(description = "정지 여부 (true=정지중)", example = "false")
        boolean isSuspended,

        @Schema(description = "가입일시", example = "2026-01-15T09:30:00")
        LocalDateTime createdAt
) {
}
