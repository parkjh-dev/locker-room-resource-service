package com.lockerroom.resourceservice.request.repository;

import com.lockerroom.resourceservice.request.model.entity.Request;
import com.lockerroom.resourceservice.request.model.enums.RequestStatus;
import com.lockerroom.resourceservice.request.model.enums.RequestType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByUserIdAndDeletedAtIsNullOrderByIdDesc(Long userId, Pageable pageable);

    List<Request> findByUserIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long userId, Long cursorId, Pageable pageable);

    @Query("SELECT r FROM Request r " +
           "JOIN FETCH r.user " +
           "WHERE r.deletedAt IS NULL " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:type IS NULL OR r.type = :type) " +
           "AND (:cursorId IS NULL OR r.id < :cursorId) " +
           "ORDER BY r.id DESC")
    List<Request> findRequestsFiltered(@Param("status") RequestStatus status,
                                        @Param("type") RequestType type,
                                        @Param("cursorId") Long cursorId,
                                        Pageable pageable);

    long countByStatusAndDeletedAtIsNull(RequestStatus status);
}
