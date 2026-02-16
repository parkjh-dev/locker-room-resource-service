package com.lockerroom.resourceservice.mapper;

import com.lockerroom.resourceservice.dto.response.AdminUserListResponse;
import com.lockerroom.resourceservice.dto.response.UserResponse;
import com.lockerroom.resourceservice.dto.response.UserTeamInfo;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.entity.UserTeam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    default UserResponse toResponse(User user, List<UserTeamInfo> teams) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getProvider(),
                teams,
                user.getCreatedAt()
        );
    }

    @Mapping(source = "team.id", target = "teamId")
    @Mapping(source = "team.name", target = "teamName")
    @Mapping(source = "sport.id", target = "sportId")
    @Mapping(source = "sport.name", target = "sportName")
    UserTeamInfo toUserTeamInfo(UserTeam userTeam);

    List<UserTeamInfo> toUserTeamInfoList(List<UserTeam> userTeams);

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
