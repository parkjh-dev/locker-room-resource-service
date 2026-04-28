package com.lockerroom.resourceservice.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "user_teams", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_teams_user_sport", columnNames = {"user_id", "sport_id"})
}, indexes = {
        @Index(name = "idx_user_teams_sport_team", columnList = "sport_id, team_id")
})
public class UserTeam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;
}
