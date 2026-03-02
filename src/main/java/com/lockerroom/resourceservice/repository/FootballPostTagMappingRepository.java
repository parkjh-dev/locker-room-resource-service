package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.FootballPostTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FootballPostTagMappingRepository extends JpaRepository<FootballPostTagMapping, Long> {
    List<FootballPostTagMapping> findByPostId(Long postId);
}
