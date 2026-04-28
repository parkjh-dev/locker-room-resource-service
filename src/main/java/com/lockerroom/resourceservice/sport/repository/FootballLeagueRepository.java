package com.lockerroom.resourceservice.sport.repository;

import com.lockerroom.resourceservice.sport.model.entity.FootballLeague;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FootballLeagueRepository extends JpaRepository<FootballLeague, Long> {
    List<FootballLeague> findByCountryId(Long countryId);
}
