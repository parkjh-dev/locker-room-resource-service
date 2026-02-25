package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Inquiry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByUserIdAndDeletedAtIsNullOrderByIdDesc(Long userId, Pageable pageable);

    List<Inquiry> findByUserIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long userId, Long cursorId, Pageable pageable);

    List<Inquiry> findByDeletedAtIsNullOrderByIdDesc(Pageable pageable);

    List<Inquiry> findByDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long cursorId, Pageable pageable);
}
