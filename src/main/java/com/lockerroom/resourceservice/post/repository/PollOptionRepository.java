package com.lockerroom.resourceservice.post.repository;

import com.lockerroom.resourceservice.post.model.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    List<PollOption> findByPollIdOrderByIdAsc(Long pollId);
}
