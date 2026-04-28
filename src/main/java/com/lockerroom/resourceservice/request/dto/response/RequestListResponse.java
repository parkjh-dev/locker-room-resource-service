package com.lockerroom.resourceservice.dto.response;

import com.lockerroom.resourceservice.model.enums.RequestStatus;
import com.lockerroom.resourceservice.model.enums.RequestType;

import java.time.LocalDateTime;

public record RequestListResponse(
        Long id,
        RequestType type,
        String name,
        RequestStatus status,
        LocalDateTime createdAt
) {
}
