package com.lockerroom.resourceservice.service;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.NotificationResponse;
import com.lockerroom.resourceservice.dto.response.UnreadCountResponse;

public interface NotificationService {

    CursorPageResponse<NotificationResponse> getMyList(Long userId, CursorPageRequest pageRequest);

    UnreadCountResponse getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    int markAllAsRead(Long userId);
}
