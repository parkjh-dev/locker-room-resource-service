package com.lockerroom.resourceservice.common.dto.response;

public record AuthorInfo(
        Long id,
        String nickname,
        String teamName,
        String profileImageUrl
) {
}
