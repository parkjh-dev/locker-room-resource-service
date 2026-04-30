-- Phase 4: 게시글 카테고리 + 투표
-- 카테고리는 3개 게시글 테이블 모두에 추가 (entity 일관성).
-- 투표 도메인은 현재 운영 endpoint(`POST /api/v1/posts`) 기준 posts 한 곳에만 매핑.
-- (FootballPost/BaseballPost endpoint 추후 신설 시 그쪽 Poll 인프라도 추가 예정)

-- ───── posts / football_posts / baseball_posts 에 category 추가 ─────
ALTER TABLE posts
    ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'GENERAL'
    COMMENT '말머리 (GENERAL/REVIEW/PREDICTION/QUESTION/MEME/NEWS)';

ALTER TABLE football_posts
    ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'GENERAL'
    COMMENT '말머리';

ALTER TABLE baseball_posts
    ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'GENERAL'
    COMMENT '말머리';

-- 결정 #10 — 게시판 type별 자동 매핑
-- QNA 게시판의 기존 글들을 QUESTION 카테고리로 일괄 변환
UPDATE posts p
   JOIN boards b ON b.id = p.board_id
   SET p.category = 'QUESTION'
 WHERE b.type = 'QNA';

-- ───── polls (Post와 1:1) ─────
CREATE TABLE polls (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    post_id         BIGINT       NOT NULL                COMMENT 'posts.id 1:1',
    question        VARCHAR(120) NULL                    COMMENT '투표 질문 (선택)',
    expires_at      DATETIME     NOT NULL                COMMENT '마감 시각',
    total_votes     INT          NOT NULL DEFAULT 0      COMMENT '캐싱: 누적 투표 수',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_polls_post UNIQUE (post_id),
    CONSTRAINT fk_polls_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    INDEX idx_polls_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ───── poll_options ─────
CREATE TABLE poll_options (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    poll_id         BIGINT       NOT NULL,
    text            VARCHAR(50)  NOT NULL                COMMENT '옵션 텍스트 (50자 이하)',
    vote_count      INT          NOT NULL DEFAULT 0      COMMENT '캐싱: 옵션별 투표 수',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME     NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_poll_options_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE,
    INDEX idx_poll_options_poll (poll_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ───── poll_votes (1인 1투표 — UNIQUE 제약으로 멱등성 보장) ─────
CREATE TABLE poll_votes (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    poll_id         BIGINT       NOT NULL,
    option_id       BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_poll_votes_poll_user UNIQUE (poll_id, user_id),
    CONSTRAINT fk_poll_votes_poll   FOREIGN KEY (poll_id)   REFERENCES polls(id)        ON DELETE CASCADE,
    CONSTRAINT fk_poll_votes_option FOREIGN KEY (option_id) REFERENCES poll_options(id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_votes_user   FOREIGN KEY (user_id)   REFERENCES users(id)        ON DELETE CASCADE,
    INDEX idx_poll_votes_option (option_id),
    INDEX idx_poll_votes_user   (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
