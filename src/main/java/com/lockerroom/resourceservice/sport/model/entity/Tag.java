package com.lockerroom.resourceservice.sport.model.entity;

import com.lockerroom.resourceservice.common.model.entity.BaseEntity;

import com.lockerroom.resourceservice.sport.model.enums.TagScope;
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
@Table(name = "tags", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tags_scope_name", columnNames = {"scope", "name"})
})
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagScope scope;

    @Column(nullable = false, length = 50)
    private String name;
}
