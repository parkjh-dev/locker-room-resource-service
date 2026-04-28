package com.lockerroom.resourceservice.post.model.entity;

import com.lockerroom.resourceservice.post.model.entity.BaseballPost;

import com.lockerroom.resourceservice.sport.model.entity.Tag;

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
@Table(name = "baseball_post_tag_mappings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_baseball_post_tag", columnNames = {"post_id", "tag_id"})
})
public class BaseballPostTagMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private BaseballPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}
