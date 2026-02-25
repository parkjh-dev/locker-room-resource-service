package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p " +
           "WHERE p.board.id = :boardId " +
           "AND p.deletedAt IS NULL " +
           "AND (:cursor IS NULL OR p.id < :cursor) " +
           "AND (:keyword IS NULL " +
           "  OR (:searchType = 'TITLE' AND p.title LIKE CONCAT('%', :keyword, '%')) " +
           "  OR (:searchType = 'CONTENT' AND p.content LIKE CONCAT('%', :keyword, '%')) " +
           "  OR (:searchType = 'TITLE_CONTENT' AND (p.title LIKE CONCAT('%', :keyword, '%') OR p.content LIKE CONCAT('%', :keyword, '%'))) " +
           "  OR (:searchType = 'NICKNAME' AND p.user.nickname LIKE CONCAT('%', :keyword, '%'))) " +
           "ORDER BY p.id DESC")
    List<Post> searchByBoard(@Param("boardId") Long boardId,
                             @Param("searchType") String searchType,
                             @Param("keyword") String keyword,
                             @Param("cursor") Long cursor,
                             Pageable pageable);

    List<Post> findByUserIdAndDeletedAtIsNullOrderByIdDesc(Long userId, Pageable pageable);

    List<Post> findByUserIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long userId, Long cursorId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL " +
           "AND (:since IS NULL OR p.createdAt >= :since) " +
           "ORDER BY p.likeCount DESC, p.id DESC")
    List<Post> findPopularPosts(@Param("since") LocalDateTime since, Pageable pageable);
}
