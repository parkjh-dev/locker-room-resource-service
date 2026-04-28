package com.lockerroom.resourceservice.kafka.event;

import java.time.LocalDateTime;

public record QnaPostCreatedEvent(
        Long postId,
        Long authorId,
        String authorNickname,
        String title,
        String content,
        LocalDateTime createdAt
) {}
