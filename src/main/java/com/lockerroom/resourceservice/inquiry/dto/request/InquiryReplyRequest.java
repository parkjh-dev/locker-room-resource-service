package com.lockerroom.resourceservice.inquiry.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "[관리자] 문의 답변 등록 요청.")
public record InquiryReplyRequest(
        @Schema(description = "답변 내용 (최대 5000자)", example = "확인해보니 일시적 문제였습니다. 다시 시도해 주세요.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 5000) String content
) {
}
