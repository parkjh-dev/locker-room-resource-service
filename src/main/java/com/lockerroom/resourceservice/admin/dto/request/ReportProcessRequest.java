package com.lockerroom.resourceservice.admin.dto.request;

import com.lockerroom.resourceservice.post.model.enums.ReportAction;
import com.lockerroom.resourceservice.post.model.enums.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "[관리자] 신고 처리 요청. status에 따라 action/suspensionDays 의미가 달라짐.")
public record ReportProcessRequest(
        @Schema(description = "처리 결과 상태 (APPROVED=조치 완료, REJECTED=기각)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull ReportStatus status,

        @Schema(description = "조치 종류 (DELETE_POST, SUSPEND_USER). status=APPROVED일 때 필요.", nullable = true)
        ReportAction action,

        @Schema(description = "정지 일수 (action=SUSPEND_USER일 때 필요, 1~365)", example = "7", nullable = true)
        @Min(1) @Max(365) Integer suspensionDays
) {
}
