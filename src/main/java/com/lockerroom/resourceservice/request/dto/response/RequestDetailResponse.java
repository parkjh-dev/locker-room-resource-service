package com.lockerroom.resourceservice.dto.response;

import com.lockerroom.resourceservice.model.enums.RequestStatus;
import com.lockerroom.resourceservice.model.enums.RequestType;

import java.time.LocalDateTime;

public record RequestDetailResponse(
        Long id,
        RequestType type,
        String name,
        String reason,
        RequestStatus status,
        String rejectReason,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
}
