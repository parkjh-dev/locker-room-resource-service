package com.lockerroom.resourceservice.board.model.enums;

public enum BoardType {
    /** 자유게시판 등 모든 사용자에게 공개되는 게시판 */
    COMMON,
    /** 질문/답변 게시판 (Q&A) */
    QNA,
    /** 공지사항 전용 (관리자 작성) */
    NOTICE,
    /** 응원팀 한정 게시판 — board.teamId 필수 */
    TEAM,
    /** 뉴스 게시판 (AI 또는 관리자 작성) */
    NEWS
}
