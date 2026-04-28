package com.lockerroom.resourceservice.dto.response;

import com.lockerroom.resourceservice.model.enums.InquiryStatus;
import com.lockerroom.resourceservice.model.enums.InquiryType;

import java.time.LocalDateTime;
import java.util.List;

public record InquiryDetailResponse(
        Long id,
        InquiryType type,
        String title,
        String content,
        InquiryStatus status,
        List<FileResponse> files,
        List<InquiryReplyResponse> replies,
        LocalDateTime createdAt
) {
}
