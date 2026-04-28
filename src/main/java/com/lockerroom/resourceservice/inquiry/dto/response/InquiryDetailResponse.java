package com.lockerroom.resourceservice.inquiry.dto.response;

import com.lockerroom.resourceservice.inquiry.dto.response.InquiryReplyResponse;

import com.lockerroom.resourceservice.file.dto.response.FileResponse;

import com.lockerroom.resourceservice.inquiry.model.enums.InquiryStatus;
import com.lockerroom.resourceservice.inquiry.model.enums.InquiryType;

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
