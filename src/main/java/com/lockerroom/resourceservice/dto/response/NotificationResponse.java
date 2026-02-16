package com.lockerroom.resourceservice.dto.response;

import com.lockerroom.resourceservice.model.enums.NotificationType;
import com.lockerroom.resourceservice.model.enums.TargetType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        TargetType targetType,
        Long targetId,
        String message,
        boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
}
