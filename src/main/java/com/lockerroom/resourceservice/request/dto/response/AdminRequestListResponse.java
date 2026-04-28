package com.lockerroom.resourceservice.request.dto.response;

import com.lockerroom.resourceservice.request.model.enums.RequestStatus;
import com.lockerroom.resourceservice.request.model.enums.RequestType;

import java.time.LocalDateTime;

public record AdminRequestListResponse(
        Long id,
        String userNickname,
        RequestType type,
        String name,
        String reason,
        RequestStatus status,
        LocalDateTime createdAt
) {
}
