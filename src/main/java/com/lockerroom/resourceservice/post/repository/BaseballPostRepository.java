package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.BaseballPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseballPostRepository extends JpaRepository<BaseballPost, Long> {
}
