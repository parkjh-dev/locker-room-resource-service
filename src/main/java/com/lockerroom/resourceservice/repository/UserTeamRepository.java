package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.UserTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTeamRepository extends JpaRepository<UserTeam, Long> {

    List<UserTeam> findByUserId(Long userId);

    Optional<UserTeam> findFirstByUserIdOrderByIdAsc(Long userId);

    List<UserTeam> findByUserIdIn(List<Long> userIds);
}
