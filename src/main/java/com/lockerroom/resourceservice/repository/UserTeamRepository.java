package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.UserTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTeamRepository extends JpaRepository<UserTeam, Long> {

    List<UserTeam> findByUserId(Long userId);

    boolean existsByUserIdAndTeamId(Long userId, Long teamId);
}
