package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Tag;
import com.lockerroom.resourceservice.model.enums.TagScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByScope(TagScope scope);
}
