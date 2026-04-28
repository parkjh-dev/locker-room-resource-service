package com.lockerroom.resourceservice.sport.model.entity;

import com.lockerroom.resourceservice.sport.model.entity.Country;

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
@Table(name = "football_leagues", indexes = {
        @Index(name = "idx_football_leagues_country", columnList = "country_id")
})
public class FootballLeague extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;

    @Column(nullable = false)
    @Builder.Default
    private int tier = 1;
}
