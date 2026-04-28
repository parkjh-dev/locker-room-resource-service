package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.BaseballTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaseballTeamRepository extends JpaRepository<BaseballTeam, Long> {
    List<BaseballTeam> findByLeagueId(Long leagueId);
}
