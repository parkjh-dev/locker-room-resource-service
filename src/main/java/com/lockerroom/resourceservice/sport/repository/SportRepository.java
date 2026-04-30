package com.lockerroom.resourceservice.sport.repository;

import com.lockerroom.resourceservice.sport.model.entity.Sport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SportRepository extends JpaRepository<Sport, Long> {

    List<Sport> findByIsActiveTrue();

    Optional<Sport> findByNameEnIgnoreCase(String nameEn);
}
