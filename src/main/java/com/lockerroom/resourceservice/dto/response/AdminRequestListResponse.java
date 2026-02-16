package com.lockerroom.resourceservice.dto.response;

import com.lockerroom.resourceservice.model.enums.RequestStatus;
import com.lockerroom.resourceservice.model.enums.RequestType;

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
