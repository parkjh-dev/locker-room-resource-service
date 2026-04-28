package com.lockerroom.resourceservice.post.repository;

import com.lockerroom.resourceservice.post.model.entity.BaseballPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseballPostRepository extends JpaRepository<BaseballPost, Long> {
}
