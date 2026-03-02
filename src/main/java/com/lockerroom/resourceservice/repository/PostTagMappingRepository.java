package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.PostTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostTagMappingRepository extends JpaRepository<PostTagMapping, Long> {
    List<PostTagMapping> findByPostId(Long postId);
}
