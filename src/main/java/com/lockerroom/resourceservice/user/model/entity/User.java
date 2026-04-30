package com.lockerroom.resourceservice.user.model.entity;

import com.lockerroom.resourceservice.common.model.entity.BaseEntity;

import com.lockerroom.resourceservice.user.model.enums.OAuthProvider;
import com.lockerroom.resourceservice.common.model.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_keycloak_id", columnNames = "keycloak_id"),
        @UniqueConstraint(name = "uk_users_phone", columnNames = "phone")
})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_id", length = 36)
    private String keycloakId;

    @Column(nullable = false)
    private String email;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(length = 20)
    private String phone;

    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;

    private String providerId;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "onboarding_completed_at")
    private LocalDateTime onboardingCompletedAt;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void updatePhone(String phone) {
        this.phone = phone;
    }

    /**
     * 온보딩 완료 시각을 idempotent하게 셋 (이미 셋된 경우 무시).
     * 응원팀 등록·온보딩 skip 양쪽에서 호출.
     */
    public void completeOnboardingIfAbsent() {
        if (this.onboardingCompletedAt == null) {
            this.onboardingCompletedAt = LocalDateTime.now();
        }
    }
}
