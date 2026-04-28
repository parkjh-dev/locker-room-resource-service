package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.FootballTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FootballTeamRepository extends JpaRepository<FootballTeam, Long> {
    List<FootballTeam> findByLeagueId(Long leagueId);
}
