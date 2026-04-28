package com.lockerroom.resourceservice.dto.response;

import com.lockerroom.resourceservice.model.enums.OAuthProvider;
import com.lockerroom.resourceservice.model.enums.Role;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        Role role,
        OAuthProvider provider,
        String profileImageUrl,
        List<UserTeamInfo> teams,
        LocalDateTime createdAt
) {
}
