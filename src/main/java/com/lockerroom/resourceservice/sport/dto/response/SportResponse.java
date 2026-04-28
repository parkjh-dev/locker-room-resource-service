package com.lockerroom.resourceservice.dto.response;

public record SportResponse(
        Long id,
        String nameKo,
        String nameEn,
        boolean isActive
) {
}
