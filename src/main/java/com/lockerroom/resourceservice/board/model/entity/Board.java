package com.lockerroom.resourceservice.board.model.entity;

import com.lockerroom.resourceservice.common.model.entity.BaseEntity;

import com.lockerroom.resourceservice.board.model.enums.BoardType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "boards", indexes = {
        @Index(name = "idx_boards_team", columnList = "team_id")
})
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardType type;

    /**
     * 팀 게시판일 때만 셋 — type=TEAM이면 NotNull. 그 외엔 null.
     * sport-agnostic 글로벌 team_id (FootballTeam·BaseballTeam이 동일 시퀀스 또는 의도적 분리).
     */
    @Column(name = "team_id")
    private Long teamId;

    /** 팀 게시판일 때 캐싱된 팀명 — 매 요청마다 join 회피용. */
    @Column(name = "team_name", length = 100)
    private String teamName;
}
