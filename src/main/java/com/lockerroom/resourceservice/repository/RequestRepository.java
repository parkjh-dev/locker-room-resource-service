package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Request;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByUserIdAndDeletedAtIsNullOrderByIdDesc(Long userId, Pageable pageable);

    List<Request> findByUserIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long userId, Long cursorId, Pageable pageable);

    List<Request> findByDeletedAtIsNullOrderByIdDesc(Pageable pageable);

    List<Request> findByDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long cursorId, Pageable pageable);
}
