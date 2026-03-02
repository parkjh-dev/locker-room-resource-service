package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Continent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContinentRepository extends JpaRepository<Continent, Long> {
}
