package com.lockerroom.resourceservice.notice.repository;

import com.lockerroom.resourceservice.notice.model.entity.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByDeletedAtIsNullOrderByIsPinnedDescCreatedAtDesc(Pageable pageable);
}
