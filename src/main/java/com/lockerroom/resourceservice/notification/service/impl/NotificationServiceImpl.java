package com.lockerroom.resourceservice.notification.service.impl;

import com.lockerroom.resourceservice.common.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.common.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.notification.dto.response.NotificationResponse;
import com.lockerroom.resourceservice.notification.dto.response.UnreadCountResponse;
import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.notification.mapper.NotificationMapper;
import com.lockerroom.resourceservice.notification.model.entity.Notification;
import com.lockerroom.resourceservice.notification.repository.NotificationRepository;
import com.lockerroom.resourceservice.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        Long cursorId = pageRequest.decodeCursor();
        Pageable pageable = PageRequest.of(0, pageRequest.getSize() + 1);

        List<Notification> notifications = (cursorId != null)
                ? notificationRepository.findByUserIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(userId, cursorId, pageable)
                : notificationRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(userId, pageable);

        boolean hasNext = notifications.size() > pageRequest.getSize();
        List<Notification> resultNotifications = hasNext
                ? notifications.subList(0, pageRequest.getSize()) : notifications;

        List<NotificationResponse> items = resultNotifications.stream()
                .map(notificationMapper::toResponse)
                .toList();

        String nextCursor = hasNext
                ? CursorPageRequest.encodeCursor(resultNotifications.get(resultNotifications.size() - 1).getId())
                : null;

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
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
    }
}
