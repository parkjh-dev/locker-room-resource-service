package com.lockerroom.resourceservice.comment.model.entity;

import com.lockerroom.resourceservice.post.model.entity.Post;

import com.lockerroom.resourceservice.user.model.entity.User;

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
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_post_created", columnList = "post_id, created_at"),
        @Index(name = "idx_comments_user", columnList = "user_id"),
        @Index(name = "idx_comments_parent", columnList = "parent_id")
})
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private boolean isAiGenerated = false;

    public void updateContent(String content) {
        this.content = content;
    }
}
