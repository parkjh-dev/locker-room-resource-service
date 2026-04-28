package com.lockerroom.resourceservice.board.model.entity;

import com.lockerroom.resourceservice.sport.model.entity.FootballTeam;

import com.lockerroom.resourceservice.common.model.entity.BaseEntity;

import com.lockerroom.resourceservice.sport.model.enums.SportBoardType;
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
@Table(name = "football_boards", indexes = {
        @Index(name = "idx_football_boards_team", columnList = "football_team_id")
})
public class FootballBoard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "football_team_id", nullable = false)
    private FootballTeam footballTeam;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SportBoardType type;
}
