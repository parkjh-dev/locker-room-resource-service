package com.lockerroom.resourceservice.inquiry.dto.response;

import com.lockerroom.resourceservice.inquiry.model.enums.InquiryStatus;
import com.lockerroom.resourceservice.inquiry.model.enums.InquiryType;

import java.time.LocalDateTime;

public record AdminInquiryListResponse(
        Long id,
        String userNickname,
        InquiryType type,
        String title,
        InquiryStatus status,
        LocalDateTime createdAt
) {
}
