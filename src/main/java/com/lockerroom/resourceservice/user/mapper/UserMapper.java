package com.lockerroom.resourceservice.user.mapper;

import com.lockerroom.resourceservice.user.dto.response.AdminUserListResponse;
import com.lockerroom.resourceservice.user.dto.response.UserResponse;
import com.lockerroom.resourceservice.user.dto.response.UserTeamInfo;
import com.lockerroom.resourceservice.user.model.entity.User;
import com.lockerroom.resourceservice.user.model.entity.UserTeam;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    default UserResponse toResponse(User user, java.util.List<UserTeamInfo> teams) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getPhone(),
                user.getNickname(),
                user.getRole(),
                user.getProvider(),
                user.getProfileImageUrl(),
                teams,
                user.getOnboardingCompletedAt(),
                user.getCreatedAt()
        );
    }

    default UserTeamInfo toUserTeamInfo(UserTeam userTeam, String teamName) {
        return new UserTeamInfo(
                userTeam.getTeamId(),
                teamName,
                userTeam.getSport().getId(),
                userTeam.getSport().getNameKo()
        );
    }

    default AdminUserListResponse toAdminListResponse(User user, boolean isSuspended) {
        return new AdminUserListResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getProvider(),
                isSuspended,
                user.getCreatedAt()
        );
    }
}
