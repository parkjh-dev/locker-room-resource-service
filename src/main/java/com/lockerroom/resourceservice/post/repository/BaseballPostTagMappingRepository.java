package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.BaseballPostTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaseballPostTagMappingRepository extends JpaRepository<BaseballPostTagMapping, Long> {
    List<BaseballPostTagMapping> findByPostId(Long postId);
}
