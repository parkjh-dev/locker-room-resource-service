package com.lockerroom.resourceservice.post.repository;

import com.lockerroom.resourceservice.post.model.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    Optional<PollVote> findByPollIdAndUserId(Long pollId, Long userId);
}
