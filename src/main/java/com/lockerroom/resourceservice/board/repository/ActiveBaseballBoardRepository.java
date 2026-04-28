package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.ActiveBaseballBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActiveBaseballBoardRepository extends JpaRepository<ActiveBaseballBoard, Long> {
    Optional<ActiveBaseballBoard> findByBaseballTeamId(Long baseballTeamId);
}
