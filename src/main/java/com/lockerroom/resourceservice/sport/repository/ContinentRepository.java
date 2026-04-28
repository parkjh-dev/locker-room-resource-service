package com.lockerroom.resourceservice.sport.repository;

import com.lockerroom.resourceservice.sport.model.entity.Continent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContinentRepository extends JpaRepository<Continent, Long> {
}
