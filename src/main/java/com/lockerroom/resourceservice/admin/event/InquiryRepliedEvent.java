package com.lockerroom.resourceservice.admin.event;

public record InquiryRepliedEvent(
        String eventId,
        Long userId,
        Long inquiryId,
        Long replyId
) {
}
