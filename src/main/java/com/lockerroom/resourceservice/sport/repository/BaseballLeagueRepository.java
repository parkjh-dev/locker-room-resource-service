package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.BaseballLeague;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaseballLeagueRepository extends JpaRepository<BaseballLeague, Long> {
    List<BaseballLeague> findByCountryId(Long countryId);
}
