package com.lockerroom.resourceservice.board.repository;

import com.lockerroom.resourceservice.board.model.entity.FootballBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FootballBoardRepository extends JpaRepository<FootballBoard, Long> {
    List<FootballBoard> findByFootballTeamId(Long footballTeamId);
}
