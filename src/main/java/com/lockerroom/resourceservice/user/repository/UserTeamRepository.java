package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.UserTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserTeamRepository extends JpaRepository<UserTeam, Long> {

    List<UserTeam> findByUserId(Long userId);

    @Query("SELECT ut FROM UserTeam ut JOIN FETCH ut.sport WHERE ut.user.id = :userId")
    List<UserTeam> findByUserIdWithSport(@Param("userId") Long userId);

    Optional<UserTeam> findFirstByUserIdOrderByIdAsc(Long userId);

    List<UserTeam> findByUserIdIn(List<Long> userIds);
}
