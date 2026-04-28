package com.lockerroom.resourceservice.notification.service;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.notification.dto.response.NotificationResponse;
import com.lockerroom.resourceservice.notification.dto.response.UnreadCountResponse;

public interface NotificationService {

    CursorPageResponse<NotificationResponse> getMyList(Long userId, CursorPageRequest pageRequest);

    UnreadCountResponse getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    int markAllAsRead(Long userId);
}
