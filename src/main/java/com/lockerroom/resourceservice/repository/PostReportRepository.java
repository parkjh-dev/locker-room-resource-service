package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.PostReport;
import com.lockerroom.resourceservice.model.enums.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    List<PostReport> findByStatusOrderByIdDesc(ReportStatus status, Pageable pageable);

    List<PostReport> findByStatusAndIdLessThanOrderByIdDesc(ReportStatus status, Long cursorId, Pageable pageable);
}
