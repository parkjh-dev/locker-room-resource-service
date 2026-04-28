package com.lockerroom.resourceservice.sport.model.entity;

import com.lockerroom.resourceservice.sport.model.entity.FootballLeague;

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
@Table(name = "football_teams", indexes = {
        @Index(name = "idx_football_teams_league", columnList = "league_id")
})
public class FootballTeam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", nullable = false)
    private FootballLeague league;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;
}
