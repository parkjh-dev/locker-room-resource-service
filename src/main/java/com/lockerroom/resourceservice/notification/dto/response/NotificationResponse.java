package com.lockerroom.resourceservice.notification.dto.response;

import com.lockerroom.resourceservice.notification.model.enums.NotificationType;
import com.lockerroom.resourceservice.file.model.enums.TargetType;

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
