package com.lockerroom.resourceservice.notification.model.entity;

import com.lockerroom.resourceservice.user.model.entity.User;

import com.lockerroom.resourceservice.common.model.entity.BaseEntity;

import com.lockerroom.resourceservice.notification.model.enums.NotificationType;
import com.lockerroom.resourceservice.file.model.enums.TargetType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user_read", columnList = "user_id, is_read")
})
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 500)
    private String message;

    @Builder.Default
    @Column(nullable = false)
    private boolean isRead = false;

    private LocalDateTime readAt;

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
