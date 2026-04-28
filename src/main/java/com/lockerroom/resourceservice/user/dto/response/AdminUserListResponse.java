package com.lockerroom.resourceservice.user.dto.response;

import com.lockerroom.resourceservice.user.model.enums.OAuthProvider;
import com.lockerroom.resourceservice.common.model.enums.Role;

import java.time.LocalDateTime;

public record AdminUserListResponse(
        Long id,
        String email,
        String nickname,
        Role role,
        OAuthProvider provider,
        boolean isSuspended,
        LocalDateTime createdAt
) {
}
