package com.lockerroom.resourceservice.notification.dto.response;

import com.lockerroom.resourceservice.file.model.enums.TargetType;
import com.lockerroom.resourceservice.notification.model.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 응답. 클릭 시 targetType+targetId로 이동.")
public record NotificationResponse(
        @Schema(description = "알림 ID", example = "888")
        Long id,

        @Schema(description = "알림 유형 (COMMENT, REPLY, INQUIRY_ANSWERED 등)")
        NotificationType type,

        @Schema(description = "이동 대상 타입 (POST, COMMENT, INQUIRY 등)")
        TargetType targetType,

        @Schema(description = "이동 대상 ID", example = "1024")
        Long targetId,

        @Schema(description = "사용자에게 노출되는 알림 메시지", example = "회원님 글에 댓글이 달렸습니다.")
        String message,

        @Schema(description = "읽음 여부", example = "false")
        boolean isRead,

        @Schema(description = "읽음 처리 일시 (미읽음이면 null)", example = "2026-04-28T10:00:00", nullable = true)
        LocalDateTime readAt,

        @Schema(description = "알림 생성 일시", example = "2026-04-28T09:30:00")
        LocalDateTime createdAt
) {
}
