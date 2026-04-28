package com.lockerroom.resourceservice.sport.dto.response;

public record SportResponse(
        Long id,
        String nameKo,
        String nameEn,
        boolean isActive
) {
}
