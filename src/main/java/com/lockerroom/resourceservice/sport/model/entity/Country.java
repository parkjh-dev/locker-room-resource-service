package com.lockerroom.resourceservice.sport.model.entity;

import com.lockerroom.resourceservice.sport.model.entity.Continent;

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
@Table(name = "countries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_countries_code", columnNames = "code")
}, indexes = {
        @Index(name = "idx_countries_continent", columnList = "continent_id")
})
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "continent_id", nullable = false)
    private Continent continent;

    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(name = "flag_url", length = 500)
    private String flagUrl;
}
