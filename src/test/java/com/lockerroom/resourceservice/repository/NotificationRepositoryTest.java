package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Notification;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.NotificationType;
import com.lockerroom.resourceservice.model.enums.Role;
import com.lockerroom.resourceservice.model.enums.TargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.lockerroom.resourceservice.configuration.JpaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaConfig.class)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("user@example.com")
                .nickname("testuser")
                .password("password123")
                .role(Role.USER)
                .build();
        entityManager.persist(user);

        otherUser = User.builder()
                .email("other@example.com")
                .nickname("otheruser")
                .password("password123")
                .role(Role.USER)
                .build();
        entityManager.persist(otherUser);

        entityManager.flush();
    }

    private Notification createNotification(User targetUser, boolean isRead) {
        return Notification.builder()
                .user(targetUser)
                .type(NotificationType.COMMENT)
                .targetType(TargetType.POST)
                .targetId(1L)
                .message("You have a new notification")
                .isRead(isRead)
                .build();
    }

    @Nested
    @DisplayName("countByUserIdAndIsReadFalse")
    class CountUnread {

        @Test
        @DisplayName("should count unread notifications for a user")
        void countUnread_success() {
            // given
            Notification unread1 = createNotification(user, false);
            Notification unread2 = createNotification(user, false);
            Notification read = createNotification(user, true);
            entityManager.persist(unread1);
            entityManager.persist(unread2);
            entityManager.persist(read);
            entityManager.flush();

            // when
            int count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should return zero when all notifications are read")
        void countUnread_allRead() {
            // given
            Notification read1 = createNotification(user, true);
            Notification read2 = createNotification(user, true);
            entityManager.persist(read1);
            entityManager.persist(read2);
            entityManager.flush();

            // when
            int count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());

            // then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("should return zero when user has no notifications")
        void countUnread_noNotifications() {
            // given - no notifications persisted

            // when
            int count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());

            // then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("should not count notifications belonging to other users")
        void countUnread_onlyOwnNotifications() {
            // given
            Notification userNotification = createNotification(user, false);
            Notification otherNotification = createNotification(otherUser, false);
            entityManager.persist(userNotification);
            entityManager.persist(otherNotification);
            entityManager.flush();

            // when
            int count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());

            // then
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("markAllAsReadByUserId")
    class MarkAllAsRead {

        @Test
        @DisplayName("should mark all unread notifications as read and return count")
        void markAllAsRead_success() {
            // given
            Notification unread1 = createNotification(user, false);
            Notification unread2 = createNotification(user, false);
            Notification alreadyRead = createNotification(user, true);
            entityManager.persist(unread1);
            entityManager.persist(unread2);
            entityManager.persist(alreadyRead);
            entityManager.flush();

            LocalDateTime now = LocalDateTime.now();

            // when
            int updatedCount = notificationRepository
                    .markAllAsReadByUserId(user.getId(), now);

            // then
            assertThat(updatedCount).isEqualTo(2);

            entityManager.clear();

            int remainingUnread = notificationRepository
                    .countByUserIdAndIsReadFalse(user.getId());
            assertThat(remainingUnread).isEqualTo(0);
        }

        @Test
        @DisplayName("should return zero when no unread notifications exist")
        void markAllAsRead_noneUnread() {
            // given
            Notification read = createNotification(user, true);
            entityManager.persist(read);
            entityManager.flush();

            LocalDateTime now = LocalDateTime.now();

            // when
            int updatedCount = notificationRepository
                    .markAllAsReadByUserId(user.getId(), now);

            // then
            assertThat(updatedCount).isEqualTo(0);
        }

        @Test
        @DisplayName("should only mark notifications for the specified user")
        void markAllAsRead_onlyTargetUser() {
            // given
            Notification userUnread = createNotification(user, false);
            Notification otherUnread = createNotification(otherUser, false);
            entityManager.persist(userUnread);
            entityManager.persist(otherUnread);
            entityManager.flush();

            LocalDateTime now = LocalDateTime.now();

            // when
            int updatedCount = notificationRepository
                    .markAllAsReadByUserId(user.getId(), now);

            // then
            assertThat(updatedCount).isEqualTo(1);

            entityManager.clear();

            int otherUnreadCount = notificationRepository
                    .countByUserIdAndIsReadFalse(otherUser.getId());
            assertThat(otherUnreadCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should set readAt timestamp on marked notifications")
        void markAllAsRead_setsReadAt() {
            // given
            Notification unread = createNotification(user, false);
            entityManager.persist(unread);
            entityManager.flush();

            LocalDateTime now = LocalDateTime.now();

            // when
            notificationRepository.markAllAsReadByUserId(user.getId(), now);
            entityManager.clear();

            // then
            Notification updated = entityManager.find(Notification.class, unread.getId());
            assertThat(updated.isRead()).isTrue();
            assertThat(updated.getReadAt()).isEqualTo(now);
        }
    }
}
