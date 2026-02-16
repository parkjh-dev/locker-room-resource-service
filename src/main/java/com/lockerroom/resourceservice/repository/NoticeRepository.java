package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByDeletedAtIsNullOrderByIsPinnedDescCreatedAtDesc(Pageable pageable);
}
