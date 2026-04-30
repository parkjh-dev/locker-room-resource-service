package com.lockerroom.resourceservice.post.repository;

import com.lockerroom.resourceservice.post.model.entity.FootballPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FootballPostRepository extends JpaRepository<FootballPost, Long> {

    /**
     * 통계 — since 이후 작성된 글을 팀별로 그룹핑한 카운트.
     * 응답: [teamId, count] 튜플 배열.
     */
    @Query("SELECT fp.board.footballTeam.id, COUNT(fp) " +
            "FROM FootballPost fp " +
            "WHERE fp.createdAt >= :since AND fp.deletedAt IS NULL " +
            "GROUP BY fp.board.footballTeam.id")
    List<Object[]> countByTeamSince(@Param("since") LocalDateTime since);
}
