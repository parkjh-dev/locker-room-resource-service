package com.lockerroom.resourceservice.dto.response;

import java.time.LocalDateTime;

public record InquiryReplyResponse(
        Long id,
        String adminNickname,
        String content,
        LocalDateTime createdAt
) {
}
