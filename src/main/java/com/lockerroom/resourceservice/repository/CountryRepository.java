package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CountryRepository extends JpaRepository<Country, Long> {
    List<Country> findByContinentId(Long continentId);
}
