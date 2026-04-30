package com.lockerroom.resourceservice.post.model.entity;

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
@Table(name = "poll_options", indexes = {
        @Index(name = "idx_poll_options_poll", columnList = "poll_id")
})
public class PollOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(nullable = false, length = 50)
    private String text;

    @Column(name = "vote_count", nullable = false)
    @Builder.Default
    private int voteCount = 0;

    public void incrementVoteCount() {
        this.voteCount++;
    }
}
