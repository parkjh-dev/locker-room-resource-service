package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Sport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportRepository extends JpaRepository<Sport, Long> {

    List<Sport> findByIsActiveTrue();
}
