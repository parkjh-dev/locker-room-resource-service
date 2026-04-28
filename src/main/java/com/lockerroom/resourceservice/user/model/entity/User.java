package com.lockerroom.resourceservice.model.entity;

import com.lockerroom.resourceservice.model.enums.OAuthProvider;
import com.lockerroom.resourceservice.model.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_keycloak_id", columnNames = "keycloak_id")
})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_id", length = 36)
    private String keycloakId;

    @Column(nullable = false)
    private String email;

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

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
