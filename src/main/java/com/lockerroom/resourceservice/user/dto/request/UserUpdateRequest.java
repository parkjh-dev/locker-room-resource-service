package com.lockerroom.resourceservice.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@Schema(description = "내 정보 수정 요청. 변경할 필드만 채워서 보내고 나머지는 기존값 유지.")
public record UserUpdateRequest(
        @Schema(description = "변경할 닉네임 (2~50자). null이면 기존값 유지.", example = "야구사랑러", nullable = true)
        @Size(min = 2, max = 50) String nickname,

        @Schema(description = "현재 비밀번호. 비밀번호 변경 시 검증용으로 필수.", example = "currentPw1234!", nullable = true)
        String currentPassword,

        @Schema(description = "변경할 새 비밀번호 (8~100자). null이면 비밀번호 미변경.", example = "newPw1234!", nullable = true)
        @Size(min = 8, max = 100) String newPassword,

        @Schema(description = "프로필 이미지 URL (사전에 /api/v1/files로 업로드 후 받은 URL). 최대 500자.", example = "https://cdn.example.com/profiles/42.jpg", nullable = true)
        @URL @Size(max = 500) String profileImageUrl
) {
}
