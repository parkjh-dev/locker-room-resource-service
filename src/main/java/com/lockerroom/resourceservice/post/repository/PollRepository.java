package com.lockerroom.resourceservice.post.repository;

import com.lockerroom.resourceservice.post.model.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {

    Optional<Poll> findByPostId(Long postId);

    boolean existsByPostId(Long postId);
}
