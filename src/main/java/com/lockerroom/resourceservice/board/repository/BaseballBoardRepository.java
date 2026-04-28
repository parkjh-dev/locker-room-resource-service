package com.lockerroom.resourceservice.board.repository;

import com.lockerroom.resourceservice.board.model.entity.BaseballBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaseballBoardRepository extends JpaRepository<BaseballBoard, Long> {
    List<BaseballBoard> findByBaseballTeamId(Long baseballTeamId);
}
