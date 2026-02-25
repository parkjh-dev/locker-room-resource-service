package com.lockerroom.resourceservice.controller;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.response.ApiResponse;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.MarkAllReadResponse;
import com.lockerroom.resourceservice.dto.response.NotificationResponse;
import com.lockerroom.resourceservice.dto.response.UnreadCountResponse;
import com.lockerroom.resourceservice.security.CurrentUserId;
import com.lockerroom.resourceservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<NotificationResponse>>> getMyList(
            @CurrentUserId Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getMyList(userId, pageRequest)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(userId)));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @CurrentUserId Long userId) {
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<MarkAllReadResponse>> markAllAsRead(
            @CurrentUserId Long userId) {
        int updatedCount = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(new MarkAllReadResponse(updatedCount)));
    }
}
