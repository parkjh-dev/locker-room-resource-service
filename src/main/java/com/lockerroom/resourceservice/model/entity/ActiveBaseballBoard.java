package com.lockerroom.resourceservice.model.entity;

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
@Table(name = "active_baseball_boards", uniqueConstraints = {
        @UniqueConstraint(name = "uk_active_baseball_boards_team", columnNames = "baseball_team_id")
})
public class ActiveBaseballBoard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baseball_team_id", nullable = false)
    private BaseballTeam baseballTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private BaseballBoard board;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
