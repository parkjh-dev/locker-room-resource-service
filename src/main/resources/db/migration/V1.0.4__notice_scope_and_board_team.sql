-- 정합성 보강
-- 1. boards 에 team_id, team_name 컬럼 (TEAM 게시판 메타)
-- 2. notices 에 scope, team_id, team_name 컬럼 (팀 한정 공지 지원)

-- ───── boards 확장 ─────
ALTER TABLE boards
    ADD COLUMN team_id BIGINT NULL COMMENT 'TEAM 게시판일 때 팀 ID',
    ADD COLUMN team_name VARCHAR(100) NULL COMMENT 'TEAM 게시판일 때 팀명 (캐싱)';
CREATE INDEX idx_boards_team ON boards(team_id);

-- ───── notices 확장 ─────
ALTER TABLE notices
    ADD COLUMN scope VARCHAR(10) NOT NULL DEFAULT 'ALL' COMMENT '공지 노출 범위 (ALL/TEAM)',
    ADD COLUMN team_id BIGINT NULL COMMENT 'scope=TEAM일 때 팀 ID',
    ADD COLUMN team_name VARCHAR(100) NULL COMMENT 'scope=TEAM일 때 팀명 (캐싱)';
CREATE INDEX idx_notices_scope_team ON notices(scope, team_id);
