# Locker Room - Database Schema Design

> 참고 문서: `back-end/decide.md` (database erd, Soft Delete 전략, 인덱스 전략)
> ERD 시각화: https://dbdiagram.io/ 에서 진행

## 공통 규칙

- Engine: InnoDB
- Charset: utf8mb4 / Collation: utf8mb4_unicode_ci
- 공통 필드: `created_at`, `updated_at`, `deleted_at` (Soft Delete)
    - 예외: `continents`, `countries` (정적 참조 테이블로 타임스탬프/Soft Delete 미적용)
- PK: BIGINT AUTO_INCREMENT
- Soft Delete: `deleted_at IS NULL` → 활성 레코드, `deleted_at IS NOT NULL` → 삭제 레코드
- ENUM은 MariaDB ENUM 타입 사용
- 시간은 모두 DATETIME (UTC 기준)

---

## DDL

### 1. 사용자

```sql
CREATE TABLE users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    email           VARCHAR(255)    NOT NULL,
    keycloak_id     VARCHAR(36)     NULL        COMMENT 'Keycloak 사용자 UUID',
    password        VARCHAR(255)    NULL        COMMENT 'SSO 유저는 NULL',
    nickname        VARCHAR(50)     NOT NULL,
    role            ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    provider        ENUM('GOOGLE', 'KAKAO', 'NAVER') NULL,
    provider_id     VARCHAR(255)    NULL        COMMENT 'SSO 고유 ID',
    profile_image_url VARCHAR(500)  NULL        COMMENT '프로필 이미지 URL',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_keycloak_id (keycloak_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE user_suspensions (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    reason          TEXT            NOT NULL,
    suspended_at    DATETIME        NOT NULL,
    suspended_until DATETIME        NOT NULL,
    admin_id        BIGINT          NOT NULL    COMMENT '처리 관리자',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_user_suspensions_user (user_id),
    CONSTRAINT fk_user_suspensions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_suspensions_admin FOREIGN KEY (admin_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE user_withdrawals (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL    COMMENT '탈퇴 사용자 ID (FK 아님, 참조용)',
    email           VARCHAR(255)    NOT NULL    COMMENT '탈퇴 시점 이메일',
    reason          TEXT            NULL,
    withdrawn_at    DATETIME        NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_user_withdrawals_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 2. 대륙

```sql
CREATE TABLE continents (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name_en         VARCHAR(50)     NOT NULL    COMMENT '영문명',
    name_ko         VARCHAR(50)     NOT NULL    COMMENT '한글명',
    code            VARCHAR(10)     NOT NULL    COMMENT '대륙 코드',
    PRIMARY KEY (id),
    UNIQUE KEY uk_continents_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 3. 국가

```sql
CREATE TABLE countries (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    continent_id    BIGINT          NOT NULL,
    name_ko         VARCHAR(100)    NOT NULL    COMMENT '한글명',
    name_en         VARCHAR(100)    NOT NULL    COMMENT '영문명',
    code            VARCHAR(10)     NOT NULL    COMMENT '국가 코드 (ISO 3166-1 alpha-2)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_countries_code (code),
    INDEX idx_countries_continent (continent_id),
    CONSTRAINT fk_countries_continent FOREIGN KEY (continent_id) REFERENCES continents (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 4. 축구 리그

```sql
CREATE TABLE football_leagues (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    country_id      BIGINT          NOT NULL,
    logo_url        VARCHAR(500)    NULL        COMMENT '리그 로고 이미지 경로 (호스트 제외)',
    name_en         VARCHAR(100)    NOT NULL    COMMENT '영문명',
    name_ko         VARCHAR(100)    NOT NULL    COMMENT '한글명',
    tier            TINYINT         NOT NULL DEFAULT 1 COMMENT '리그 등급 (1부, 2부 등)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_football_leagues_country (country_id),
    CONSTRAINT fk_football_leagues_country FOREIGN KEY (country_id) REFERENCES countries (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 5. 축구 팀

```sql
CREATE TABLE football_teams (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    league_id       BIGINT          NOT NULL,
    logo_url        VARCHAR(500)    NULL        COMMENT '팀 로고 이미지 경로 (호스트 제외)',
    name_en         VARCHAR(100)    NOT NULL    COMMENT '영문명',
    name_ko         VARCHAR(100)    NOT NULL    COMMENT '한글명',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_football_teams_league (league_id),
    CONSTRAINT fk_football_teams_league FOREIGN KEY (league_id) REFERENCES football_leagues (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 6. 축구 게시판/게시글

```sql
CREATE TABLE football_boards (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    football_team_id    BIGINT          NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    type                ENUM('TEAM', 'NEWS') NOT NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_football_boards_team (football_team_id),
    CONSTRAINT fk_football_boards_team FOREIGN KEY (football_team_id) REFERENCES football_teams (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE football_posts (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    board_id        BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL    COMMENT '작성자',
    title           VARCHAR(200)    NOT NULL,
    content         TEXT            NOT NULL,
    view_count      INT             NOT NULL DEFAULT 0,
    like_count      INT             NOT NULL DEFAULT 0   COMMENT '좋아요 수 (캐싱)',
    comment_count   INT             NOT NULL DEFAULT 0   COMMENT '댓글 수 (캐싱)',
    is_ai_generated TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_football_posts_board_created (board_id, created_at),
    INDEX idx_football_posts_user (user_id),
    CONSTRAINT fk_football_posts_board FOREIGN KEY (board_id) REFERENCES football_boards (id),
    CONSTRAINT fk_football_posts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE active_football_boards (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    football_team_id    BIGINT          NOT NULL,
    board_id            BIGINT          NULL,
    is_active           TINYINT(1)      NOT NULL DEFAULT 1,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_active_football_boards_team (football_team_id),
    CONSTRAINT fk_active_football_boards_team FOREIGN KEY (football_team_id) REFERENCES football_teams (id),
    CONSTRAINT fk_active_football_boards_board FOREIGN KEY (board_id) REFERENCES football_boards (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 7. 야구 리그

```sql
CREATE TABLE baseball_leagues (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    country_id      BIGINT          NOT NULL,
    logo_url        VARCHAR(500)    NULL        COMMENT '리그 로고 이미지 경로 (호스트 제외)',
    name_en         VARCHAR(100)    NOT NULL    COMMENT '영문명',
    name_ko         VARCHAR(100)    NOT NULL    COMMENT '한글명',
    tier            TINYINT         NOT NULL DEFAULT 1 COMMENT '리그 등급 (1군, 2군 등)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_baseball_leagues_country (country_id),
    CONSTRAINT fk_baseball_leagues_country FOREIGN KEY (country_id) REFERENCES countries (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 8. 야구 팀

```sql
CREATE TABLE baseball_teams (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    league_id       BIGINT          NOT NULL,
    logo_url        VARCHAR(500)    NULL        COMMENT '팀 로고 이미지 경로 (호스트 제외)',
    name_en         VARCHAR(100)    NOT NULL    COMMENT '영문명',
    name_ko         VARCHAR(100)    NOT NULL    COMMENT '한글명',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_baseball_teams_league (league_id),
    CONSTRAINT fk_baseball_teams_league FOREIGN KEY (league_id) REFERENCES baseball_leagues (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 9. 야구 게시판/게시글

```sql
CREATE TABLE baseball_boards (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    baseball_team_id    BIGINT          NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    type                ENUM('TEAM', 'NEWS') NOT NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_baseball_boards_team (baseball_team_id),
    CONSTRAINT fk_baseball_boards_team FOREIGN KEY (baseball_team_id) REFERENCES baseball_teams (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE baseball_posts (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    board_id        BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL    COMMENT '작성자',
    title           VARCHAR(200)    NOT NULL,
    content         TEXT            NOT NULL,
    view_count      INT             NOT NULL DEFAULT 0,
    like_count      INT             NOT NULL DEFAULT 0   COMMENT '좋아요 수 (캐싱)',
    comment_count   INT             NOT NULL DEFAULT 0   COMMENT '댓글 수 (캐싱)',
    is_ai_generated TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_baseball_posts_board_created (board_id, created_at),
    INDEX idx_baseball_posts_user (user_id),
    CONSTRAINT fk_baseball_posts_board FOREIGN KEY (board_id) REFERENCES baseball_boards (id),
    CONSTRAINT fk_baseball_posts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE active_baseball_boards (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    baseball_team_id    BIGINT          NOT NULL,
    board_id            BIGINT          NULL,
    is_active           TINYINT(1)      NOT NULL DEFAULT 1,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_active_baseball_boards_team (baseball_team_id),
    CONSTRAINT fk_active_baseball_boards_team FOREIGN KEY (baseball_team_id) REFERENCES baseball_teams (id),
    CONSTRAINT fk_active_baseball_boards_board FOREIGN KEY (board_id) REFERENCES baseball_boards (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 10. 태그

```sql
CREATE TABLE tags (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    scope       ENUM('COMMON', 'SPORT') NOT NULL,
    name        VARCHAR(50)     NOT NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tags_scope_name (scope, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 11. 종목

```sql
CREATE TABLE sports (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name_ko         VARCHAR(50)     NOT NULL    COMMENT '종목명 국문 (축구, 야구 등)',
    name_en         VARCHAR(50)     NOT NULL    COMMENT '종목명 영문 (Football, Baseball 등)',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 12. 사용자 응원팀

```sql
CREATE TABLE user_teams (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    sport_id        BIGINT          NOT NULL,
    team_id         BIGINT          NOT NULL    COMMENT '종목별 팀 ID (폴리모픽, FK 없음)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_teams_user_sport (user_id, sport_id),
    INDEX idx_user_teams_sport_team (sport_id, team_id),
    CONSTRAINT fk_user_teams_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_teams_sport FOREIGN KEY (sport_id) REFERENCES sports (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 13. 게시판/게시글

```sql
CREATE TABLE boards (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL,
    type            ENUM('COMMON', 'QNA', 'NOTICE') NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE posts (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    board_id        BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL    COMMENT '작성자',
    title           VARCHAR(200)    NOT NULL,
    content         TEXT            NOT NULL,
    view_count      INT             NOT NULL DEFAULT 0,
    like_count      INT             NOT NULL DEFAULT 0   COMMENT '좋아요 수 (캐싱)',
    comment_count   INT             NOT NULL DEFAULT 0   COMMENT '댓글 수 (캐싱)',
    is_ai_generated TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_posts_board_created (board_id, created_at),
    INDEX idx_posts_user (user_id),
    CONSTRAINT fk_posts_board FOREIGN KEY (board_id) REFERENCES boards (id),
    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE post_likes (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    post_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_likes_post_user (post_id, user_id),
    CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE post_reports (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    post_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL    COMMENT '신고자',
    reason          TEXT            NOT NULL,
    status          ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    processed_at    DATETIME        NULL,
    admin_id        BIGINT          NULL        COMMENT '처리 관리자',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_post_reports_status (status),
    INDEX idx_post_reports_post (post_id),
    CONSTRAINT fk_post_reports_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_post_reports_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_post_reports_admin FOREIGN KEY (admin_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 14. 태그 매핑

```sql
CREATE TABLE post_tag_mappings (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    post_id     BIGINT      NOT NULL,
    tag_id      BIGINT      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_tag (post_id, tag_id),
    CONSTRAINT fk_ptm_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_ptm_tag  FOREIGN KEY (tag_id)  REFERENCES tags (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE football_post_tag_mappings (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    post_id     BIGINT      NOT NULL,
    tag_id      BIGINT      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_football_post_tag (post_id, tag_id),
    CONSTRAINT fk_fptm_post FOREIGN KEY (post_id) REFERENCES football_posts (id),
    CONSTRAINT fk_fptm_tag  FOREIGN KEY (tag_id)  REFERENCES tags (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE baseball_post_tag_mappings (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    post_id     BIGINT      NOT NULL,
    tag_id      BIGINT      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_baseball_post_tag (post_id, tag_id),
    CONSTRAINT fk_bptm_post FOREIGN KEY (post_id) REFERENCES baseball_posts (id),
    CONSTRAINT fk_bptm_tag  FOREIGN KEY (tag_id)  REFERENCES tags (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 15. 댓글

```sql
CREATE TABLE comments (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    post_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    parent_id       BIGINT          NULL        COMMENT '대댓글인 경우 부모 댓글 ID',
    content         TEXT            NOT NULL,
    is_ai_generated TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_comments_post_created (post_id, created_at),
    INDEX idx_comments_user (user_id),
    INDEX idx_comments_parent (parent_id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 16. 공지사항

```sql
CREATE TABLE notices (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200)    NOT NULL,
    content         TEXT            NOT NULL,
    is_pinned       TINYINT(1)      NOT NULL DEFAULT 0,
    admin_id        BIGINT          NOT NULL    COMMENT '작성 관리자',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notices_admin FOREIGN KEY (admin_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 17. 고객센터

```sql
CREATE TABLE inquiries (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    type            ENUM('GENERAL', 'BUG', 'SUGGESTION') NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    content         TEXT            NOT NULL,
    status          ENUM('PENDING', 'ANSWERED') NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_inquiries_user (user_id),
    INDEX idx_inquiries_status (status),
    CONSTRAINT fk_inquiries_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE inquiry_replies (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    inquiry_id      BIGINT          NOT NULL,
    admin_id        BIGINT          NOT NULL,
    content         TEXT            NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_inquiry_replies_inquiry (inquiry_id),
    CONSTRAINT fk_inquiry_replies_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries (id),
    CONSTRAINT fk_inquiry_replies_admin FOREIGN KEY (admin_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 18. 요청

```sql
CREATE TABLE requests (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    type            ENUM('SPORT', 'TEAM') NOT NULL,
    name            VARCHAR(100)    NOT NULL    COMMENT '요청 종목/구단명',
    reason          TEXT            NOT NULL,
    status          ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    processed_at    DATETIME        NULL,
    admin_id        BIGINT          NULL        COMMENT '처리 관리자',
    reject_reason   TEXT            NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_requests_user (user_id),
    INDEX idx_requests_status (status),
    CONSTRAINT fk_requests_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_requests_admin FOREIGN KEY (admin_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 19. 파일

```sql
CREATE TABLE files (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL    COMMENT '업로더',
    target_type     ENUM('POST', 'INQUIRY', 'COMMENT', 'PROFILE') NOT NULL,
    target_id       BIGINT          NOT NULL,
    original_name   VARCHAR(255)    NOT NULL,
    stored_name     VARCHAR(255)    NOT NULL,
    s3_path         VARCHAR(500)    NOT NULL,
    size            BIGINT          NOT NULL    COMMENT '파일 크기 (bytes)',
    mime_type       VARCHAR(100)    NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_files_target (target_type, target_id),
    INDEX idx_files_user (user_id),
    CONSTRAINT fk_files_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 20. 알림

```sql
CREATE TABLE notifications (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL    COMMENT '수신자',
    type            ENUM('COMMENT', 'REPLY', 'NOTICE', 'INQUIRY_REPLY', 'REPORT_PROCESSED') NOT NULL,
    target_type     ENUM('POST', 'COMMENT', 'NOTICE', 'INQUIRY') NOT NULL,
    target_id       BIGINT          NOT NULL,
    message         VARCHAR(500)    NOT NULL,
    is_read         TINYINT(1)      NOT NULL DEFAULT 0,
    read_at         DATETIME        NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_notifications_user_read (user_id, is_read),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 테이블 생성 순서

FK 의존성을 고려한 실행 순서:

```
1. users
2. continents
3. countries
4. football_leagues
5. football_teams
6. football_boards
7. football_posts
8. active_football_boards
9. baseball_leagues
10. baseball_teams
11. baseball_boards
12. baseball_posts
13. active_baseball_boards
14. tags
15. sports
16. user_teams
17. user_suspensions
18. user_withdrawals
19. boards
20. posts
21. post_likes
22. post_reports
23. post_tag_mappings
24. football_post_tag_mappings
25. baseball_post_tag_mappings
26. comments
27. notices
28. inquiries
29. inquiry_replies
30. requests
31. files
32. notifications
```

---

## 인덱스 요약

| 테이블 | 인덱스명 | 컬럼 | 타입 |
|--------|----------|------|------|
| user_teams | uk_user_teams_user_sport | user_id, sport_id | UNIQUE |
| user_teams | idx_user_teams_sport_team | sport_id, team_id | INDEX |
| users | uk_users_email | email | UNIQUE |
| continents | uk_continents_code | code | UNIQUE |
| countries | uk_countries_code | code | UNIQUE |
| countries | idx_countries_continent | continent_id | INDEX |
| football_leagues | idx_football_leagues_country | country_id | INDEX |
| football_teams | idx_football_teams_league | league_id | INDEX |
| football_boards | idx_football_boards_team | football_team_id | INDEX |
| football_posts | idx_football_posts_board_created | board_id, created_at | INDEX |
| football_posts | idx_football_posts_user | user_id | INDEX |
| active_football_boards | uk_active_football_boards_team | football_team_id | UNIQUE |
| baseball_leagues | idx_baseball_leagues_country | country_id | INDEX |
| baseball_teams | idx_baseball_teams_league | league_id | INDEX |
| baseball_boards | idx_baseball_boards_team | baseball_team_id | INDEX |
| baseball_posts | idx_baseball_posts_board_created | board_id, created_at | INDEX |
| baseball_posts | idx_baseball_posts_user | user_id | INDEX |
| active_baseball_boards | uk_active_baseball_boards_team | baseball_team_id | UNIQUE |
| posts | idx_posts_board_created | board_id, created_at | INDEX |
| posts | idx_posts_user | user_id | INDEX |
| post_likes | uk_post_likes_post_user | post_id, user_id | UNIQUE |
| post_reports | idx_post_reports_status | status | INDEX |
| post_reports | idx_post_reports_post | post_id | INDEX |
| comments | idx_comments_post_created | post_id, created_at | INDEX |
| comments | idx_comments_user | user_id | INDEX |
| comments | idx_comments_parent | parent_id | INDEX |
| inquiries | idx_inquiries_user | user_id | INDEX |
| inquiries | idx_inquiries_status | status | INDEX |
| requests | idx_requests_user | user_id | INDEX |
| requests | idx_requests_status | status | INDEX |
| files | idx_files_target | target_type, target_id | INDEX |
| files | idx_files_user | user_id | INDEX |
| tags | uk_tags_scope_name | scope, name | UNIQUE |
| post_tag_mappings | uk_post_tag | post_id, tag_id | UNIQUE |
| football_post_tag_mappings | uk_football_post_tag | post_id, tag_id | UNIQUE |
| baseball_post_tag_mappings | uk_baseball_post_tag | post_id, tag_id | UNIQUE |
| notifications | idx_notifications_user_read | user_id, is_read | INDEX |

---

## 초기 데이터 (Seed)

### Init 파일 실행 순서

`docker-entrypoint-initdb.d`에서 파일명 알파벳 순으로 실행된다. 숫자 접두사로 FK 의존성 순서를 보장한다.

| 순서 | 파일명 | 내용 |
|------|--------|------|
| 01 | `01_init.sql` | 테이블 32개 생성 (DDL만) |
| 02 | `02_seed.sql` | 공통 시드 (관리자, 종목, 게시판, 태그) |
| 03 | `03_continent.sql` | 대륙 데이터 |
| 04 | `04_countries.sql` | 국가 데이터 |
| 05 | `05_football_leagues.sql` | 축구 리그 데이터 |
| 06 | `06_football_teams.sql` | 축구 팀 데이터 |
| 07 | `07_football_boards.sql` | 축구 게시판 + 활성화 데이터 |
| 08 | `08_baseball_leagues.sql` | 야구 리그 데이터 |
| 09 | `09_baseball_teams.sql` | 야구 팀 데이터 |
| 10 | `10_baseball_boards.sql` | 야구 게시판 + 활성화 데이터 |
| 11 | `11_dummy.sql` | 더미 데이터 (개발용, user_teams 포함) |

### 파일별 시드 내용

| 파일 | 대상 테이블 | 데이터 |
|------|------------|--------|
| `02_seed.sql` | `users` | 관리자 계정 1건 |
| | `sports` | 종목 2건 (축구, 야구) |
| | `boards` | 공통 3건 (COMMON, QNA, NOTICE) |
| | `tags` | COMMON 3건 (자유, 질문, 정보), SPORT 3건 (경기후기, 이적, 하이라이트) |
| `03_continent.sql` | `continents` | 대륙 데이터 |
| `04_countries.sql` | `countries` | 국가 데이터 |
| `05_football_leagues.sql` | `football_leagues` | 축구 리그 데이터 (CSV 기반) |
| `06_football_teams.sql` | `football_teams` | 축구 팀 데이터 (CSV 기반) |
| `07_football_boards.sql` | `football_boards` | `INSERT...SELECT`로 팀당 TEAM/NEWS 게시판 2건 자동 생성 |
| | `active_football_boards` | `INSERT...SELECT`로 팀당 활성화 1건 자동 생성 |
| `08_baseball_leagues.sql` | `baseball_leagues` | 야구 리그 데이터 (CSV 기반) |
| `09_baseball_teams.sql` | `baseball_teams` | 야구 팀 데이터 (CSV 기반) |
| `10_baseball_boards.sql` | `baseball_boards` | `INSERT...SELECT`로 팀당 TEAM/NEWS 게시판 2건 자동 생성 |
| | `active_baseball_boards` | `INSERT...SELECT`로 팀당 활성화 1건 자동 생성 |
| `11_dummy.sql` | `user_teams` | 더미 사용자 응원팀 8건 (서브쿼리로 팀 이름 기반 매핑) |
