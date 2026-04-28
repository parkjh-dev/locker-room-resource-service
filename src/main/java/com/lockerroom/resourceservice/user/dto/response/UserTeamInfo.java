package com.lockerroom.resourceservice.user.dto.response;

public record UserTeamInfo(
        Long teamId,
        String teamName,
        Long sportId,
        String sportName
) {
}
