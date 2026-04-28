package com.lockerroom.resourceservice.inquiry.dto.response;

import com.lockerroom.resourceservice.inquiry.model.enums.InquiryStatus;
import com.lockerroom.resourceservice.inquiry.model.enums.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "[관리자] 문의 목록 행 응답. 문의자 닉네임 포함.")
public record AdminInquiryListResponse(
        @Schema(description = "문의 ID", example = "300")
        Long id,

        @Schema(description = "문의자 닉네임", example = "야구사랑러")
        String userNickname,

        @Schema(description = "문의 유형")
        InquiryType type,

        @Schema(description = "문의 제목", example = "로그인이 안 됩니다")
        String title,

        @Schema(description = "처리 상태")
        InquiryStatus status,

        @Schema(description = "문의 등록 일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt
) {
}
