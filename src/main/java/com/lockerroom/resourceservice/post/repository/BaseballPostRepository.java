package com.lockerroom.resourceservice.post.repository;

import com.lockerroom.resourceservice.post.model.entity.BaseballPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BaseballPostRepository extends JpaRepository<BaseballPost, Long> {

    /**
     * 통계 — since 이후 작성된 글을 팀별로 그룹핑한 카운트.
     * 응답: [teamId, count] 튜플 배열.
     */
    @Query("SELECT bp.board.baseballTeam.id, COUNT(bp) " +
            "FROM BaseballPost bp " +
            "WHERE bp.createdAt >= :since AND bp.deletedAt IS NULL " +
            "GROUP BY bp.board.baseballTeam.id")
    List<Object[]> countByTeamSince(@Param("since") LocalDateTime since);
}
