package com.lockerroom.resourceservice.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "전체 읽음 처리 결과. 일괄 read 처리된 행 수.")
public record MarkAllReadResponse(
        @Schema(description = "이번 요청으로 읽음 처리된 알림 개수", example = "5")
        int updatedCount
) {
}
