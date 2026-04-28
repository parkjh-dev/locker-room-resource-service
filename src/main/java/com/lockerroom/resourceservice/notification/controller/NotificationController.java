package com.lockerroom.resourceservice.notification.controller;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.common.dto.response.ApiResponse;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.infrastructure.security.CurrentUserId;
import com.lockerroom.resourceservice.notification.dto.response.MarkAllReadResponse;
import com.lockerroom.resourceservice.notification.dto.response.NotificationResponse;
import com.lockerroom.resourceservice.notification.dto.response.UnreadCountResponse;
import com.lockerroom.resourceservice.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "알림", description = "사용자 알림 목록 조회, 미읽음 카운트, 읽음 처리")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "내 알림 목록", description = "본인의 알림을 커서 페이지네이션으로 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<NotificationResponse>>> getMyList(
            @CurrentUserId Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getMyList(userId, pageRequest)));
    }

    @Operation(summary = "미읽음 알림 카운트", description = "본인의 읽지 않은 알림 개수를 반환합니다 (UI 뱃지용).")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(userId)));
    }

    @Operation(summary = "알림 읽음 처리", description = "지정 알림을 읽음으로 표시합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "NOTIFICATION_NOT_FOUND")
    })
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @CurrentUserId Long userId) {
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("알림을 읽음 처리했습니다."));
    }

    @Operation(summary = "전체 알림 읽음 처리", description = "본인의 모든 미읽음 알림을 일괄 읽음 처리합니다. 처리된 개수를 반환.")
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<MarkAllReadResponse>> markAllAsRead(
            @CurrentUserId Long userId) {
        int updatedCount = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(new MarkAllReadResponse(updatedCount)));
    }
}
