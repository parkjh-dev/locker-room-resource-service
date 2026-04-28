package com.lockerroom.resourceservice.board.model.entity;

import com.lockerroom.resourceservice.sport.model.entity.BaseballTeam;

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
@Table(name = "baseball_boards", indexes = {
        @Index(name = "idx_baseball_boards_team", columnList = "baseball_team_id")
})
public class BaseballBoard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baseball_team_id", nullable = false)
    private BaseballTeam baseballTeam;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SportBoardType type;
}
