package com.lockerroom.resourceservice.dto.response;

public record AuthorInfo(
        Long id,
        String nickname,
        String teamName,
        String profileImageUrl
) {
}
