package com.lockerroom.resourceservice.post.model.entity;

import com.lockerroom.resourceservice.user.model.entity.User;

import com.lockerroom.resourceservice.board.model.entity.BaseballBoard;

import com.lockerroom.resourceservice.common.model.entity.BaseEntity;

import com.lockerroom.resourceservice.post.model.enums.PostCategory;

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
@Table(name = "baseball_posts", indexes = {
        @Index(name = "idx_baseball_posts_board_created", columnList = "board_id, created_at"),
        @Index(name = "idx_baseball_posts_user", columnList = "user_id")
})
public class BaseballPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BaseballBoard board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PostCategory category = PostCategory.GENERAL;

    @Column(nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int commentCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean isAiGenerated = false;
}
