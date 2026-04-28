package com.lockerroom.resourceservice.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글/댓글 등 콘텐츠 작성자 정보. 응답 어디서나 동일 구조로 직렬화.")
public record AuthorInfo(
        @Schema(description = "작성자 사용자 ID", example = "42")
        Long id,

        @Schema(description = "작성자 닉네임", example = "야구사랑러")
        String nickname,

        @Schema(description = "작성자가 응원하는 팀명. 미설정 시 null.", example = "두산 베어스", nullable = true)
        String teamName,

        @Schema(description = "프로필 이미지 URL. 미설정 시 null.", example = "https://cdn.example.com/profiles/42.jpg", nullable = true)
        String profileImageUrl
) {
}
