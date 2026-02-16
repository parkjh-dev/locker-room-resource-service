package com.lockerroom.resourceservice.dto.response;

import com.lockerroom.resourceservice.model.enums.BoardType;

public record BoardResponse(
        Long id,
        String name,
        BoardType type,
        Long teamId,
        String teamName
) {
}
