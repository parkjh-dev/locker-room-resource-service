package com.lockerroom.resourceservice.dto.response;

public record TeamResponse(
        Long id,
        String name,
        String logoUrl,
        boolean isActive
) {
}
