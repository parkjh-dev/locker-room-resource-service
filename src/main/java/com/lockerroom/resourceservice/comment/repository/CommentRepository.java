package com.lockerroom.resourceservice.comment.repository;

import com.lockerroom.resourceservice.comment.model.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdAndParentIsNullAndDeletedAtIsNullOrderByIdAsc(Long postId, Pageable pageable);

    List<Comment> findByPostIdAndParentIsNullAndDeletedAtIsNullAndIdGreaterThanOrderByIdAsc(
            Long postId, Long cursorId, Pageable pageable);

    List<Comment> findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long parentId);

    List<Comment> findByParentIdInAndDeletedAtIsNullOrderByParentIdAscCreatedAtAsc(List<Long> parentIds);

    List<Comment> findByUserIdAndDeletedAtIsNullOrderByIdDesc(Long userId, Pageable pageable);

    List<Comment> findByUserIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long userId, Long cursorId, Pageable pageable);

    int countByPostIdAndDeletedAtIsNull(Long postId);
}
