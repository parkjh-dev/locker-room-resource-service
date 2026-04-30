-- Phase 2: User 도메인 확장
-- 이메일 인증·휴대폰 번호·온보딩 완료 시각 컬럼 추가.

-- ───── users 컬럼 추가 ─────
ALTER TABLE users
    ADD COLUMN email_verified TINYINT(1) NOT NULL DEFAULT 0 AFTER email,
    ADD COLUMN phone VARCHAR(20) NULL AFTER email_verified,
    ADD COLUMN onboarding_completed_at DATETIME NULL AFTER profile_image_url;

-- ───── 기존 사용자 처리 (결정 #3) ─────
-- 운영 사용자는 모두 신뢰 가정 → email_verified = TRUE 일괄 처리
UPDATE users SET email_verified = 1;

-- ───── phone UNIQUE 제약 (결정 #4) ─────
-- 동일 번호 1계정 정책. NULL은 다중 허용됨 (MariaDB 기본 동작).
ALTER TABLE users
    ADD CONSTRAINT uk_users_phone UNIQUE (phone);
