package com.lockerroom.resourceservice.post.model.enums;

import com.lockerroom.resourceservice.board.model.enums.BoardType;

/**
 * 게시글 말머리(카테고리). 프론트 목록·상세에서 뱃지로 표시.
 */
public enum PostCategory {
    GENERAL,    // 일반
    REVIEW,     // 후기
    PREDICTION, // 예측
    QUESTION,   // 질문
    MEME,       // 짤방
    NEWS;       // 뉴스

    /**
     * 게시판 타입에 맞는 default 카테고리.
     * - QNA 게시판 → QUESTION
     * - 그 외(COMMON, NOTICE) → GENERAL
     */
    public static PostCategory defaultFor(BoardType boardType) {
        if (boardType == null) return GENERAL;
        return switch (boardType) {
            case QNA -> QUESTION;
            default -> GENERAL;
        };
    }
}
