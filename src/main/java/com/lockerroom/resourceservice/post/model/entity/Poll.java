package com.lockerroom.resourceservice.post.model.entity;

import com.lockerroom.resourceservice.common.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "polls", uniqueConstraints = {
        @UniqueConstraint(name = "uk_polls_post", columnNames = "post_id")
}, indexes = {
        @Index(name = "idx_polls_expires", columnList = "expires_at")
})
public class Poll extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(length = 120)
    private String question;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "total_votes", nullable = false)
    @Builder.Default
    private int totalVotes = 0;

    public void incrementTotalVotes() {
        this.totalVotes++;
    }
}
