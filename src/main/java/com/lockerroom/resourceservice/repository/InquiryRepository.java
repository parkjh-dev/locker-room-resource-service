package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Inquiry;
import com.lockerroom.resourceservice.model.enums.InquiryStatus;
import com.lockerroom.resourceservice.model.enums.InquiryType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByUserIdAndDeletedAtIsNullOrderByIdDesc(Long userId, Pageable pageable);

    List<Inquiry> findByUserIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long userId, Long cursorId, Pageable pageable);

    List<Inquiry> findByDeletedAtIsNullOrderByIdDesc(Pageable pageable);

    List<Inquiry> findByDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long cursorId, Pageable pageable);

    @Query("SELECT i FROM Inquiry i WHERE i.deletedAt IS NULL " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:type IS NULL OR i.type = :type) " +
           "AND (:cursorId IS NULL OR i.id < :cursorId) " +
           "ORDER BY i.id DESC")
    List<Inquiry> findInquiriesFiltered(@Param("status") InquiryStatus status,
                                        @Param("type") InquiryType type,
                                        @Param("cursorId") Long cursorId,
                                        Pageable pageable);

    long countByStatusAndDeletedAtIsNull(InquiryStatus status);
}
