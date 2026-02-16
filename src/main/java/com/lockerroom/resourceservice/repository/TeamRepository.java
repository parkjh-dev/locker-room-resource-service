package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findBySportIdAndIsActiveTrue(Long sportId);
}
