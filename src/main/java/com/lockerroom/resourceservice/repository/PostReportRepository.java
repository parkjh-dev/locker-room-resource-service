package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.PostReport;
import com.lockerroom.resourceservice.model.enums.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    List<PostReport> findByStatusOrderByIdDesc(ReportStatus status, Pageable pageable);

    List<PostReport> findByStatusAndIdLessThanOrderByIdDesc(ReportStatus status, Long cursorId, Pageable pageable);

    @Query("SELECT r FROM PostReport r WHERE " +
           "(:status IS NULL OR r.status = :status) " +
           "AND (:cursorId IS NULL OR r.id < :cursorId) " +
           "ORDER BY r.id DESC")
    List<PostReport> findReportsFiltered(@Param("status") ReportStatus status,
                                         @Param("cursorId") Long cursorId,
                                         Pageable pageable);

    long countByStatusAndDeletedAtIsNull(ReportStatus status);
}
