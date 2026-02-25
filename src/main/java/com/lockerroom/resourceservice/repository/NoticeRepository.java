package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByDeletedAtIsNullOrderByIsPinnedDescCreatedAtDesc(Pageable pageable);

    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL " +
           "AND (:teamId IS NULL OR n.team.id = :teamId) " +
           "ORDER BY n.isPinned DESC, n.createdAt DESC")
    List<Notice> findFilteredNotices(@Param("teamId") Long teamId, Pageable pageable);
}
