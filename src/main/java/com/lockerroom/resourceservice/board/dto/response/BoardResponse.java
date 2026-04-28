package com.lockerroom.resourceservice.board.dto.response;

import com.lockerroom.resourceservice.board.model.enums.BoardType;

public record BoardResponse(
        Long id,
        String name,
        BoardType type
) {
}
