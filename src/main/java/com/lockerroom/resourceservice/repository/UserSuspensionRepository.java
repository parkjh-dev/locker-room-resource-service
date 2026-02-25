package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.UserSuspension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface UserSuspensionRepository extends JpaRepository<UserSuspension, Long> {

    @Query("SELECT us FROM UserSuspension us " +
           "WHERE us.user.id = :userId " +
           "AND us.suspendedUntil > :now " +
           "AND us.deletedAt IS NULL")
    Optional<UserSuspension> findActiveByUserId(@Param("userId") Long userId,
                                                @Param("now") OffsetDateTime now);
}
