package com.lockerroom.resourceservice.file.dto.response;

public record FileResponse(
        Long id,
        String originalName,
        String url,
        long size,
        String mimeType
) {
}
