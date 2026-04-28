package com.lockerroom.resourceservice.kafka.event;

import com.lockerroom.resourceservice.model.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationEvent(
        Long recipientId,
        Long senderId,
        String senderNickname,
        NotificationType type,
        Long targetId,
        String message,
        LocalDateTime createdAt
) {}
