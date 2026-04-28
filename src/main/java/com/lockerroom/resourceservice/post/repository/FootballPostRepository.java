package com.lockerroom.resourceservice.post.repository;

import com.lockerroom.resourceservice.post.model.entity.FootballPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FootballPostRepository extends JpaRepository<FootballPost, Long> {
}
