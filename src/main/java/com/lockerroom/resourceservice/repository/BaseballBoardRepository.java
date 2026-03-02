package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.BaseballBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaseballBoardRepository extends JpaRepository<BaseballBoard, Long> {
    List<BaseballBoard> findByBaseballTeamId(Long baseballTeamId);
}
