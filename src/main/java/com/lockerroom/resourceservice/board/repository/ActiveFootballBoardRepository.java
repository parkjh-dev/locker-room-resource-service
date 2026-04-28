package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.ActiveFootballBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActiveFootballBoardRepository extends JpaRepository<ActiveFootballBoard, Long> {
    Optional<ActiveFootballBoard> findByFootballTeamId(Long footballTeamId);
}
