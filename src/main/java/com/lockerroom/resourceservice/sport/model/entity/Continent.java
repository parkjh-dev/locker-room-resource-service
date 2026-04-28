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
@Table(name = "continents", uniqueConstraints = {
        @UniqueConstraint(name = "uk_continents_code", columnNames = "code")
})
public class Continent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_en", nullable = false, length = 50)
    private String nameEn;

    @Column(name = "name_ko", nullable = false, length = 50)
    private String nameKo;

    @Column(nullable = false, length = 10)
    private String code;
}
