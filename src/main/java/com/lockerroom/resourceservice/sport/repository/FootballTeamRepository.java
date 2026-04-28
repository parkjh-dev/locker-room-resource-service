package com.lockerroom.resourceservice.sport.repository;

import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FootballTeamRepository extends JpaRepository<FootballTeam, Long> {
    List<FootballTeam> findByLeagueId(Long leagueId);
}
