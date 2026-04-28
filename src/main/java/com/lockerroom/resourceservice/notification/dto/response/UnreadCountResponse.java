package com.lockerroom.resourceservice.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미읽음 알림 카운트 응답 (UI 뱃지용).")
public record UnreadCountResponse(
        @Schema(description = "현재 미읽음 알림 개수", example = "3")
        int unreadCount
) {
}
