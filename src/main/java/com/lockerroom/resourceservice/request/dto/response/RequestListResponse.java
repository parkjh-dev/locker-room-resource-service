package com.lockerroom.resourceservice.request.dto.response;

import com.lockerroom.resourceservice.request.model.enums.RequestStatus;
import com.lockerroom.resourceservice.request.model.enums.RequestType;

import java.time.LocalDateTime;

public record RequestListResponse(
        Long id,
        RequestType type,
        String name,
        RequestStatus status,
        LocalDateTime createdAt
) {
}
