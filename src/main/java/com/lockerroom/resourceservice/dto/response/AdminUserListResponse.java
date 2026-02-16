package com.lockerroom.resourceservice.dto.response;

import com.lockerroom.resourceservice.model.enums.OAuthProvider;
import com.lockerroom.resourceservice.model.enums.Role;

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
