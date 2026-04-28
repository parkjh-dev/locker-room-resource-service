package com.lockerroom.resourceservice.user.dto.response;

import com.lockerroom.resourceservice.common.model.enums.Role;
import com.lockerroom.resourceservice.user.model.enums.OAuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "사용자 프로필 응답. /me 조회/수정 시 반환.")
public record UserResponse(
        @Schema(description = "사용자 ID", example = "42")
        Long id,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "닉네임", example = "야구사랑러")
        String nickname,

        @Schema(description = "권한 (USER, ADMIN)")
        Role role,

        @Schema(description = "OAuth 제공자 (GOOGLE, KAKAO 등). 자체 가입 사용자는 LOCAL.")
        OAuthProvider provider,

        @Schema(description = "프로필 이미지 URL. 미설정 시 null.", example = "https://cdn.example.com/profiles/42.jpg", nullable = true)
        String profileImageUrl,

        @Schema(description = "응원하는 팀 목록. 종목 여러 개 등록 가능.")
        List<UserTeamInfo> teams,

        @Schema(description = "가입일시", example = "2026-01-15T09:30:00")
        LocalDateTime createdAt
) {
}
