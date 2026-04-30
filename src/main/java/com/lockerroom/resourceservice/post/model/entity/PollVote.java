package com.lockerroom.resourceservice.post.model.entity;

import com.lockerroom.resourceservice.common.model.entity.BaseEntity;
import com.lockerroom.resourceservice.user.model.entity.User;
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
@Table(name = "poll_votes", uniqueConstraints = {
        // 1인 1투표 — 멱등성 처리는 DB UNIQUE + 예외 catch 패턴
        @UniqueConstraint(name = "uk_poll_votes_poll_user", columnNames = {"poll_id", "user_id"})
}, indexes = {
        @Index(name = "idx_poll_votes_option", columnList = "option_id"),
        @Index(name = "idx_poll_votes_user", columnList = "user_id")
})
public class PollVote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private PollOption option;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
