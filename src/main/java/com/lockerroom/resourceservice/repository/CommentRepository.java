package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(Long postId);

    List<Comment> findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long parentId);

    List<Comment> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

    int countByPostIdAndDeletedAtIsNull(Long postId);
}
