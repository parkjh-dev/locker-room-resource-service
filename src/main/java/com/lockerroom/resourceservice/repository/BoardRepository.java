package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Board;
import com.lockerroom.resourceservice.model.enums.BoardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findByTypeIn(List<BoardType> types);

    List<Board> findByTeamId(Long teamId);
}
