package com.lockerroom.resourceservice.inquiry.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 답변 응답.")
public record InquiryReplyResponse(
        @Schema(description = "답변 ID", example = "150")
        Long id,

        @Schema(description = "답변한 관리자 닉네임", example = "운영자")
        String adminNickname,

        @Schema(description = "답변 내용", example = "확인해보니 일시적 문제였습니다. 다시 시도해 주세요.")
        String content,

        @Schema(description = "답변 등록 일시", example = "2026-04-28T11:00:00")
        LocalDateTime createdAt
) {
}
