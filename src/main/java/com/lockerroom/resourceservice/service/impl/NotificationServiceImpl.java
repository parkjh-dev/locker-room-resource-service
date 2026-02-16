package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.NotificationResponse;
import com.lockerroom.resourceservice.dto.response.UnreadCountResponse;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.mapper.NotificationMapper;
import com.lockerroom.resourceservice.model.entity.Notification;
import com.lockerroom.resourceservice.repository.NotificationRepository;
import com.lockerroom.resourceservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public CursorPageResponse<NotificationResponse> getMyList(Long userId, CursorPageRequest pageRequest) {
        List<Notification> notifications = notificationRepository
                .findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        userId, PageRequest.of(0, pageRequest.getSize() + 1));

        boolean hasNext = notifications.size() > pageRequest.getSize();
        List<Notification> resultNotifications = hasNext
                ? notifications.subList(0, pageRequest.getSize()) : notifications;

        List<NotificationResponse> items = resultNotifications.stream()
                .map(notificationMapper::toResponse)
                .toList();

        String nextCursor = hasNext ? String.valueOf(resultNotifications.get(resultNotifications.size() - 1).getId()) : null;

        return CursorPageResponse.<NotificationResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    @Override
    public UnreadCountResponse getUnreadCount(Long userId) {
        int count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new UnreadCountResponse(count);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        notification.markAsRead();
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
    }
}
