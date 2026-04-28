package com.lockerroom.resourceservice.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "[관리자] 대시보드 통계. 미처리 카운트 묶음.")
public record AdminDashboardResponse(
        @Schema(description = "처리 대기중인 신고 수", example = "5")
        long pendingReportCount,

        @Schema(description = "답변 대기중인 문의 수", example = "12")
        long pendingInquiryCount,

        @Schema(description = "처리 대기중인 사용자 요청 수", example = "3")
        long pendingRequestCount
) {
}
