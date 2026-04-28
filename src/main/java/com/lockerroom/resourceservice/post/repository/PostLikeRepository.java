package com.lockerroom.resourceservice.post.repository;

import com.lockerroom.resourceservice.post.model.entity.PostLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    int countByPostId(Long postId);

    @Query("SELECT pl FROM PostLike pl JOIN FETCH pl.post p JOIN FETCH p.board JOIN FETCH p.user " +
           "WHERE pl.user.id = :userId AND p.deletedAt IS NULL " +
           "AND (:cursor IS NULL OR pl.id < :cursor) " +
           "ORDER BY pl.id DESC")
    List<PostLike> findByUserIdWithPost(@Param("userId") Long userId,
                                        @Param("cursor") Long cursor,
                                        Pageable pageable);
}
