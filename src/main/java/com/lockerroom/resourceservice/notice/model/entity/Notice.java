package com.lockerroom.resourceservice.notice.model.entity;

import com.lockerroom.resourceservice.user.model.entity.User;

import com.lockerroom.resourceservice.common.model.entity.BaseEntity;

import com.lockerroom.resourceservice.notice.model.enums.NoticeScope;

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
@Table(name = "notices", indexes = {
        @Index(name = "idx_notices_scope_team", columnList = "scope, team_id")
})
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPinned = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private NoticeScope scope = NoticeScope.ALL;

    /** scope=TEAM일 때만 셋. 그 외엔 null. */
    @Column(name = "team_id")
    private Long teamId;

    /** scope=TEAM일 때 캐싱된 팀명. */
    @Column(name = "team_name", length = 100)
    private String teamName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateIsPinned(boolean isPinned) {
        this.isPinned = isPinned;
    }

    public void updateScope(NoticeScope scope, Long teamId, String teamName) {
        this.scope = scope;
        this.teamId = (scope == NoticeScope.TEAM) ? teamId : null;
        this.teamName = (scope == NoticeScope.TEAM) ? teamName : null;
    }
}
