package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Request;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Request> findByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);
}
