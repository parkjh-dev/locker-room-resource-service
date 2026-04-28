package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.FootballPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FootballPostRepository extends JpaRepository<FootballPost, Long> {
}
