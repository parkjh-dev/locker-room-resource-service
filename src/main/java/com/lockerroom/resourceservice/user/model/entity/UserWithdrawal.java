package com.lockerroom.resourceservice.user.model.entity;

import com.lockerroom.resourceservice.common.model.entity.BaseEntity;

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
@Table(name = "user_withdrawals", indexes = {
        @Index(name = "idx_user_withdrawals_user", columnList = "user_id")
})
public class UserWithdrawal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private LocalDateTime withdrawnAt;
}
