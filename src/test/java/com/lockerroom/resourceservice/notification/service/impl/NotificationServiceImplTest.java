package com.lockerroom.resourceservice.service.impl;

import com.lockerroom.resourceservice.dto.request.CursorPageRequest;
import com.lockerroom.resourceservice.dto.response.CursorPageResponse;
import com.lockerroom.resourceservice.dto.response.NotificationResponse;
import com.lockerroom.resourceservice.dto.response.UnreadCountResponse;
import com.lockerroom.resourceservice.exceptions.CustomException;
import com.lockerroom.resourceservice.exceptions.ErrorCode;
import com.lockerroom.resourceservice.mapper.NotificationMapper;
import com.lockerroom.resourceservice.model.entity.Notification;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.NotificationType;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.model.enums.TargetType;
import com.lockerroom.resourceservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;

    @InjectMocks private NotificationServiceImpl notificationService;

    private User user;
    private User otherUser;
    private Notification notification;
    private Notification readNotification;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .nickname("testuser")
                .role(Role.USER)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@test.com")
                .nickname("otheruser")
                .role(Role.USER)
                .build();

        notification = Notification.builder()
                .id(1L)
                .user(user)
                .type(NotificationType.COMMENT)
                .targetType(TargetType.POST)
                .targetId(10L)
                .message("Someone commented on your post")
                .build();

        readNotification = Notification.builder()
                .id(2L)
                .user(user)
                .type(NotificationType.REPLY)
                .targetType(TargetType.POST)
                .targetId(10L)
                .message("Someone replied to your comment")
                .isRead(true)
                .build();
    }

    @Nested
    @DisplayName("getMyList")
    class GetMyList {

        @Test
        @DisplayName("should return paginated notifications with hasNext=true")
        void getMyList_hasNext() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(1);

            NotificationResponse response1 = new NotificationResponse(
                    1L, NotificationType.COMMENT, TargetType.POST, 10L,
                    "Someone commented on your post", false, null, null
            );

            when(notificationRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class)))
                    .thenReturn(List.of(notification, readNotification));
            when(notificationMapper.toResponse(notification)).thenReturn(response1);

            CursorPageResponse<NotificationResponse> result = notificationService.getMyList(1L, pageRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getNextCursor()).isEqualTo(CursorPageRequest.encodeCursor(1L));
        }

        @Test
        @DisplayName("should return paginated notifications with hasNext=false")
        void getMyList_noNext() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(10);

            NotificationResponse response1 = new NotificationResponse(
                    1L, NotificationType.COMMENT, TargetType.POST, 10L,
                    "Someone commented", false, null, null
            );
            NotificationResponse response2 = new NotificationResponse(
                    2L, NotificationType.REPLY, TargetType.POST, 10L,
                    "Someone replied", true, LocalDateTime.now(), null
            );

            when(notificationRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class)))
                    .thenReturn(List.of(notification, readNotification));
            when(notificationMapper.toResponse(notification)).thenReturn(response1);
            when(notificationMapper.toResponse(readNotification)).thenReturn(response2);

            CursorPageResponse<NotificationResponse> result = notificationService.getMyList(1L, pageRequest);

            assertThat(result.getItems()).hasSize(2);
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("should return empty list when no notifications")
        void getMyList_empty() {
            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(10);

            when(notificationRepository.findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                    eq(1L), any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            CursorPageResponse<NotificationResponse> result = notificationService.getMyList(1L, pageRequest);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("should return unread count")
        void getUnreadCount_success() {
            when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5);

            UnreadCountResponse result = notificationService.getUnreadCount(1L);

            assertThat(result).isNotNull();
            assertThat(result.unreadCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("should return zero when no unread notifications")
        void getUnreadCount_zero() {
            when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(0);

            UnreadCountResponse result = notificationService.getUnreadCount(1L);

            assertThat(result.unreadCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("should mark notification as read successfully")
        void markAsRead_success() {
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

            notificationService.markAsRead(1L, 1L);

            assertThat(notification.isRead()).isTrue();
            assertThat(notification.getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw exception when notification not found")
        void markAsRead_notFound_throwsException() {
            when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class,
                    () -> notificationService.markAsRead(999L, 1L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw exception when user is not the notification owner")
        void markAsRead_forbidden_throwsException() {
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

            CustomException exception = assertThrows(CustomException.class,
                    () -> notificationService.markAsRead(1L, 2L));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("should mark all notifications as read")
        void markAllAsRead_success() {
            when(notificationRepository.markAllAsReadByUserId(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(3);

            int result = notificationService.markAllAsRead(1L);

            assertThat(result).isEqualTo(3);
            verify(notificationRepository).markAllAsReadByUserId(eq(1L), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("should handle case when no unread notifications exist")
        void markAllAsRead_noUnread() {
            when(notificationRepository.markAllAsReadByUserId(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(0);

            int result = notificationService.markAllAsRead(1L);

            assertThat(result).isEqualTo(0);
            verify(notificationRepository).markAllAsReadByUserId(eq(1L), any(LocalDateTime.class));
        }
    }
}
