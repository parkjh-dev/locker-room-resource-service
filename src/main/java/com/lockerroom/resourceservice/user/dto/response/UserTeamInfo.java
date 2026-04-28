package com.lockerroom.resourceservice.dto.response;

public record UserTeamInfo(
        Long teamId,
        String teamName,
        Long sportId,
        String sportName
) {
}
