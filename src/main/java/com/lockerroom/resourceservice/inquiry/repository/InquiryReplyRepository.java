package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.InquiryReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryReplyRepository extends JpaRepository<InquiryReply, Long> {

    List<InquiryReply> findByInquiryIdOrderByCreatedAtAsc(Long inquiryId);
}
