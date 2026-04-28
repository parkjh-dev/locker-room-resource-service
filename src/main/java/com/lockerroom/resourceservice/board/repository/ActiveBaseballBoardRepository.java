package com.lockerroom.resourceservice.board.repository;

import com.lockerroom.resourceservice.board.model.entity.ActiveBaseballBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActiveBaseballBoardRepository extends JpaRepository<ActiveBaseballBoard, Long> {
    Optional<ActiveBaseballBoard> findByBaseballTeamId(Long baseballTeamId);
}
