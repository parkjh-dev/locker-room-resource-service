package com.lockerroom.resourceservice.post.model.entity;

import com.lockerroom.resourceservice.post.model.entity.Post;

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
@Table(name = "post_tag_mappings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_tag", columnNames = {"post_id", "tag_id"})
})
public class PostTagMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}
