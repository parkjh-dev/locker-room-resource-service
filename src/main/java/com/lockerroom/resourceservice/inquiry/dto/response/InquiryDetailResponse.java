package com.lockerroom.resourceservice.inquiry.dto.response;

import com.lockerroom.resourceservice.file.dto.response.FileResponse;
import com.lockerroom.resourceservice.inquiry.model.enums.InquiryStatus;
import com.lockerroom.resourceservice.inquiry.model.enums.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "문의 상세 응답. 첨부파일과 답변 목록 포함.")
public record InquiryDetailResponse(
        @Schema(description = "문의 ID", example = "300")
        Long id,

        @Schema(description = "문의 유형")
        InquiryType type,

        @Schema(description = "문의 제목", example = "로그인이 안 됩니다")
        String title,

        @Schema(description = "문의 내용", example = "구글 OAuth 로그인 시 오류가 발생합니다.")
        String content,

        @Schema(description = "처리 상태 (PENDING, ANSWERED)")
        InquiryStatus status,

        @Schema(description = "첨부파일 목록")
        List<FileResponse> files,

        @Schema(description = "관리자 답변 목록 (없으면 빈 배열)")
        List<InquiryReplyResponse> replies,

        @Schema(description = "문의 등록 일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt
) {
}
