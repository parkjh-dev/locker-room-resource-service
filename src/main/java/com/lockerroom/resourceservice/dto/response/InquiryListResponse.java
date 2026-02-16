package com.lockerroom.resourceservice.dto.response;

import com.lockerroom.resourceservice.model.enums.InquiryStatus;
import com.lockerroom.resourceservice.model.enums.InquiryType;

import java.time.LocalDateTime;

public record InquiryListResponse(
        Long id,
        InquiryType type,
        String title,
        InquiryStatus status,
        LocalDateTime createdAt
) {
}
