package com.lockerroom.resourceservice.board.model.entity;

import com.lockerroom.resourceservice.board.model.entity.FootballBoard;

import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;

import com.lockerroom.resourceservice.common.model.entity.BaseEntity;

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
@Table(name = "active_football_boards", uniqueConstraints = {
        @UniqueConstraint(name = "uk_active_football_boards_team", columnNames = "football_team_id")
})
public class ActiveFootballBoard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "football_team_id", nullable = false)
    private FootballTeam footballTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private FootballBoard board;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
