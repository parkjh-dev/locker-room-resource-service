package com.lockerroom.resourceservice.user.repository;

import com.lockerroom.resourceservice.user.model.entity.UserTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserTeamRepository extends JpaRepository<UserTeam, Long> {

    List<UserTeam> findByUserId(Long userId);

    @Query("SELECT ut FROM UserTeam ut JOIN FETCH ut.sport WHERE ut.user.id = :userId")
    List<UserTeam> findByUserIdWithSport(@Param("userId") Long userId);

    Optional<UserTeam> findFirstByUserIdOrderByIdAsc(Long userId);

    List<UserTeam> findByUserIdIn(List<Long> userIds);

    /** 종목별 락 정책 — 사용자가 해당 종목에 이미 등록한 팀이 있는지 확인. */
    boolean existsByUserIdAndSportId(Long userId, Long sportId);

    /** 종목·팀별 응원자(팔로워) 수. */
    @Query("SELECT COUNT(ut) FROM UserTeam ut WHERE ut.sport.id = :sportId AND ut.teamId = :teamId")
    long countBySportIdAndTeamId(@Param("sportId") Long sportId, @Param("teamId") Long teamId);

    /**
     * 종목 내 팀별 응원자 수를 한 번에 집계. Stats 모듈의 N+1 회피용.
     * 응답: [teamId, count] 튜플 배열.
     */
    @Query("SELECT ut.teamId, COUNT(ut) FROM UserTeam ut " +
            "WHERE ut.sport.id = :sportId GROUP BY ut.teamId")
    List<Object[]> countFollowersBySport(@Param("sportId") Long sportId);
}
