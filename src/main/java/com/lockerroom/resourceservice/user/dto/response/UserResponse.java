package com.lockerroom.resourceservice.user.dto.response;

import com.lockerroom.resourceservice.user.dto.response.UserTeamInfo;

import com.lockerroom.resourceservice.user.model.enums.OAuthProvider;
import com.lockerroom.resourceservice.common.model.enums.Role;

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
