package com.lockerroom.resourceservice.notice.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지 노출 범위.")
public enum NoticeScope {
    /** 모든 사용자에게 노출 */
    ALL,
    /** 특정 팀 응원자에게만 노출 — notice.teamId 필수 */
    TEAM
}
