package com.lockerroom.resourceservice.dto.response;

public record FileResponse(
        Long id,
        String originalName,
        String url,
        long size,
        String mimeType
) {
}
