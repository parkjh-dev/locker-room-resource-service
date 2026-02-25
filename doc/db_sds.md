# Locker Room - Database Schema Design

> 참고 문서: `back-end/decide.md` (database erd, Soft Delete 전략, 인덱스 전략)
> ERD 시각화: https://dbdiagram.io/ 에서 진행

## 공통 규칙

- Engine: InnoDB
- Charset: utf8mb4 / Collation: utf8mb4_unicode_ci
- 공통 필드: `created_at`, `updated_at`, `deleted_at` (Soft Delete)
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
    keycloak_id     VARCHAR(36)     NULL        COMMENT 'Keycloak UUID (JWT sub claim)',
    email           VARCHAR(255)    NOT NULL,
    password        VARCHAR(255)    NULL        COMMENT 'SSO 유저는 NULL',
    nickname        VARCHAR(50)     NOT NULL,
    role            ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    provider        ENUM('GOOGLE', 'KAKAO', 'NAVER') NULL,
    provider_id     VARCHAR(255)    NULL        COMMENT 'SSO 고유 ID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_keycloak_id (keycloak_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE user_teams (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    team_id         BIGINT          NOT NULL,
    sport_id        BIGINT          NOT NULL    COMMENT '팀의 종목 (비정규화)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_teams_user_sport (user_id, sport_id),
    CONSTRAINT fk_user_teams_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_teams_team FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT fk_user_teams_sport FOREIGN KEY (sport_id) REFERENCES sports (id)
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

### 2. 종목/팀

```sql
CREATE TABLE sports (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(50)     NOT NULL    COMMENT '종목명 (축구, 야구 등)',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE teams (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    sport_id        BIGINT          NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    logo_url        VARCHAR(500)    NULL,
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_teams_sport (sport_id),
    CONSTRAINT fk_teams_sport FOREIGN KEY (sport_id) REFERENCES sports (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 3. 게시판/게시글

```sql
CREATE TABLE boards (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL,
    type            ENUM('TEAM', 'COMMON', 'QNA', 'NOTICE', 'NEWS') NOT NULL,
    team_id         BIGINT          NULL        COMMENT '팀 전용 게시판인 경우',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_boards_team (team_id),
    CONSTRAINT fk_boards_team FOREIGN KEY (team_id) REFERENCES teams (id)
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

### 4. 댓글

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

### 5. 공지사항

```sql
CREATE TABLE notices (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200)    NOT NULL,
    content         TEXT            NOT NULL,
    is_pinned       TINYINT(1)      NOT NULL DEFAULT 0,
    scope           ENUM('ALL', 'TEAM') NOT NULL DEFAULT 'ALL',
    team_id         BIGINT          NULL        COMMENT '팀 공지인 경우',
    admin_id        BIGINT          NOT NULL    COMMENT '작성 관리자',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (id),
    INDEX idx_notices_team (team_id),
    CONSTRAINT fk_notices_team FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT fk_notices_admin FOREIGN KEY (admin_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 6. 고객센터

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

### 7. 요청

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

### 8. 파일

```sql
CREATE TABLE files (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL    COMMENT '업로더',
    target_type     ENUM('POST', 'INQUIRY', 'COMMENT') NOT NULL,
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

### 9. 알림

```sql
CREATE TABLE notifications (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL    COMMENT '수신자',
    type            ENUM('COMMENT', 'REPLY', 'NOTICE', 'INQUIRY_REPLY') NOT NULL,
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
2. sports
3. teams
4. user_teams
5. user_suspensions
6. user_withdrawals
7. boards
8. posts
9. post_likes
10. post_reports
11. comments
12. notices
13. inquiries
14. inquiry_replies
15. requests
16. files
17. notifications
```

---

## 인덱스 요약

| 테이블 | 인덱스명 | 컬럼 | 타입 |
|--------|----------|------|------|
| users | uk_users_email | email | UNIQUE |
| users | uk_users_keycloak_id | keycloak_id | UNIQUE |
| user_teams | uk_user_teams_user_sport | user_id, sport_id | UNIQUE |
| teams | idx_teams_sport | sport_id | INDEX |
| boards | idx_boards_team | team_id | INDEX |
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
| notifications | idx_notifications_user_read | user_id, is_read | INDEX |

---

## 초기 데이터 (Seed)

```sql
-- 종목
INSERT INTO sports (name, is_active) VALUES ('축구', 1);
INSERT INTO sports (name, is_active) VALUES ('야구', 1);

-- 축구 팀 (K리그1 기준)
INSERT INTO teams (sport_id, name, is_active) VALUES (1, '울산 HD FC', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (1, '김천 상무 FC', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (1, '전북 현대 모터스', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (1, 'FC 서울', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (1, '포항 스틸러스', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (1, '수원 FC', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (1, '제주 유나이티드', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (1, '인천 유나이티드', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (1, '대전 하나 시티즌', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (1, '강원 FC', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (1, '대구 FC', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (1, '광주 FC', 1);

-- 야구 팀 (KBO 기준)
INSERT INTO teams (sport_id, name, is_active) VALUES (2, '삼성 라이온즈', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (2, 'LG 트윈스', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (2, 'KT 위즈', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (2, 'SSG 랜더스', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (2, 'NC 다이노스', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (2, '두산 베어스', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (2, 'KIA 타이거즈', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (2, '롯데 자이언츠', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (2, '한화 이글스', 1);
INSERT INTO teams (sport_id, name, is_active) VALUES (2, '키움 히어로즈', 1);

-- 공통 게시판
INSERT INTO boards (name, type, team_id) VALUES ('공통 게시판', 'COMMON', NULL);
INSERT INTO boards (name, type, team_id) VALUES ('Q&A 게시판', 'QNA', NULL);
INSERT INTO boards (name, type, team_id) VALUES ('공지사항', 'NOTICE', NULL);

-- 팀 전용 게시판 (각 팀별 자동 생성)
-- 축구 팀 게시판
INSERT INTO boards (name, type, team_id) VALUES ('울산 HD FC 게시판', 'TEAM', 1);
INSERT INTO boards (name, type, team_id) VALUES ('김천 상무 FC 게시판', 'TEAM', 2);
INSERT INTO boards (name, type, team_id) VALUES ('전북 현대 모터스 게시판', 'TEAM', 3);
INSERT INTO boards (name, type, team_id) VALUES ('FC 서울 게시판', 'TEAM', 4);
INSERT INTO boards (name, type, team_id) VALUES ('포항 스틸러스 게시판', 'TEAM', 5);
INSERT INTO boards (name, type, team_id) VALUES ('수원 FC 게시판', 'TEAM', 6);
INSERT INTO boards (name, type, team_id) VALUES ('제주 유나이티드 게시판', 'TEAM', 7);
INSERT INTO boards (name, type, team_id) VALUES ('인천 유나이티드 게시판', 'TEAM', 8);
INSERT INTO boards (name, type, team_id) VALUES ('대전 하나 시티즌 게시판', 'TEAM', 9);
INSERT INTO boards (name, type, team_id) VALUES ('강원 FC 게시판', 'TEAM', 10);
INSERT INTO boards (name, type, team_id) VALUES ('대구 FC 게시판', 'TEAM', 11);
INSERT INTO boards (name, type, team_id) VALUES ('광주 FC 게시판', 'TEAM', 12);

-- 야구 팀 게시판
INSERT INTO boards (name, type, team_id) VALUES ('삼성 라이온즈 게시판', 'TEAM', 13);
INSERT INTO boards (name, type, team_id) VALUES ('LG 트윈스 게시판', 'TEAM', 14);
INSERT INTO boards (name, type, team_id) VALUES ('KT 위즈 게시판', 'TEAM', 15);
INSERT INTO boards (name, type, team_id) VALUES ('SSG 랜더스 게시판', 'TEAM', 16);
INSERT INTO boards (name, type, team_id) VALUES ('NC 다이노스 게시판', 'TEAM', 17);
INSERT INTO boards (name, type, team_id) VALUES ('두산 베어스 게시판', 'TEAM', 18);
INSERT INTO boards (name, type, team_id) VALUES ('KIA 타이거즈 게시판', 'TEAM', 19);
INSERT INTO boards (name, type, team_id) VALUES ('롯데 자이언츠 게시판', 'TEAM', 20);
INSERT INTO boards (name, type, team_id) VALUES ('한화 이글스 게시판', 'TEAM', 21);
INSERT INTO boards (name, type, team_id) VALUES ('키움 히어로즈 게시판', 'TEAM', 22);

-- 팀 뉴스 게시판 (AI 자동 생성용)
-- 축구 팀 뉴스
INSERT INTO boards (name, type, team_id) VALUES ('울산 HD FC 뉴스', 'NEWS', 1);
INSERT INTO boards (name, type, team_id) VALUES ('김천 상무 FC 뉴스', 'NEWS', 2);
INSERT INTO boards (name, type, team_id) VALUES ('전북 현대 모터스 뉴스', 'NEWS', 3);
INSERT INTO boards (name, type, team_id) VALUES ('FC 서울 뉴스', 'NEWS', 4);
INSERT INTO boards (name, type, team_id) VALUES ('포항 스틸러스 뉴스', 'NEWS', 5);
INSERT INTO boards (name, type, team_id) VALUES ('수원 FC 뉴스', 'NEWS', 6);
INSERT INTO boards (name, type, team_id) VALUES ('제주 유나이티드 뉴스', 'NEWS', 7);
INSERT INTO boards (name, type, team_id) VALUES ('인천 유나이티드 뉴스', 'NEWS', 8);
INSERT INTO boards (name, type, team_id) VALUES ('대전 하나 시티즌 뉴스', 'NEWS', 9);
INSERT INTO boards (name, type, team_id) VALUES ('강원 FC 뉴스', 'NEWS', 10);
INSERT INTO boards (name, type, team_id) VALUES ('대구 FC 뉴스', 'NEWS', 11);
INSERT INTO boards (name, type, team_id) VALUES ('광주 FC 뉴스', 'NEWS', 12);

-- 야구 팀 뉴스
INSERT INTO boards (name, type, team_id) VALUES ('삼성 라이온즈 뉴스', 'NEWS', 13);
INSERT INTO boards (name, type, team_id) VALUES ('LG 트윈스 뉴스', 'NEWS', 14);
INSERT INTO boards (name, type, team_id) VALUES ('KT 위즈 뉴스', 'NEWS', 15);
INSERT INTO boards (name, type, team_id) VALUES ('SSG 랜더스 뉴스', 'NEWS', 16);
INSERT INTO boards (name, type, team_id) VALUES ('NC 다이노스 뉴스', 'NEWS', 17);
INSERT INTO boards (name, type, team_id) VALUES ('두산 베어스 뉴스', 'NEWS', 18);
INSERT INTO boards (name, type, team_id) VALUES ('KIA 타이거즈 뉴스', 'NEWS', 19);
INSERT INTO boards (name, type, team_id) VALUES ('롯데 자이언츠 뉴스', 'NEWS', 20);
INSERT INTO boards (name, type, team_id) VALUES ('한화 이글스 뉴스', 'NEWS', 21);
INSERT INTO boards (name, type, team_id) VALUES ('키움 히어로즈 뉴스', 'NEWS', 22);

-- 관리자 계정
INSERT INTO users (email, password, nickname, role) VALUES ('admin@lockerroom.com', '$2a$10$PLACEHOLDER_BCRYPT_HASH', '관리자', 'ADMIN');
```
