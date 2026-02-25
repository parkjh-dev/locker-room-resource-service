# Locker Room Backend - 소프트웨어 요구사항 명세서 (SRS)

> 참고 문서: `back-end/decide.md`, `back-end/api.md`, `back-end/sds.md`
> 본 문서는 백엔드 시스템 관점의 요구사항을 정의한다.

---

## 1. 개요

### 1.1 목적
본 문서는 Locker Room 백엔드 시스템의 기능적/비기능적 요구사항을 서비스 단위로 정의한다. 각 마이크로서비스가 충족해야 할 기술 요구사항, 서비스 간 통신 규약, 데이터 처리 요구사항을 기술한다.

### 1.2 시스템 범위

```
Client → API Gateway(:8080) → auth-service(:8081)
                             → resource-service(:8082)
                             → ai-service(:8083)
                                    ↕ Kafka ↕
                             → notification-service(:8084)

Infrastructure: MariaDB / Redis / Kafka / AWS S3 / LLM API
```

### 1.3 서비스별 책임

| 서비스 | 책임 | Gateway 연결 |
|--------|------|:------------:|
| gateway | 라우팅, Rate Limiting, CORS, 토큰 사전 검증 | - |
| auth-service | 회원가입, 로그인, SSO, 토큰 관리, 비밀번호 재설정 | O |
| resource-service | 사용자, 게시판, 게시글, 댓글, 공지, 고객센터, 알림, 파일, 관리자 | O |
| ai-service | AI 뉴스 생성, Q&A 자동 답변, LLM 연동 | O |
| notification-service | 이메일/SMS 발송, 인앱 알림 저장 (Kafka Consumer) | X |

---

## 2. Gateway Service 요구사항

### 2.1 라우팅

| ID | 요구사항 | 상세 |
|----|----------|------|
| GW-RT-001 | Path 기반 라우팅 | `/api/v1/auth/**` → auth-service, `/api/v1/ai/**` → ai-service, `/api/v1/**` → resource-service |
| GW-RT-002 | 라우팅 우선순위 | 구체적 경로 우선 매칭 (ai > auth > resource) |
| GW-RT-003 | 서비스 헬스체크 | 라우팅 대상 서비스 상태 확인, 비정상 시 503 응답 |

### 2.2 Rate Limiting

| ID | 요구사항 | 상세 |
|----|----------|------|
| GW-RL-001 | 비인증 사용자 제한 | 60 req/min, IP 기준 |
| GW-RL-002 | 인증 사용자 제한 | 120 req/min, userId 기준 |
| GW-RL-003 | 좋아요/신고 제한 | 30 req/min, userId + path 기준 |
| GW-RL-004 | 로그인/회원가입 제한 | 10 req/min, IP 기준 |
| GW-RL-005 | Rate Limit 저장소 | Redis 기반 Token Bucket 또는 Sliding Window |
| GW-RL-006 | 초과 시 응답 | HTTP 429 + `COMMON_RATE_LIMIT_EXCEEDED` 에러 코드 |

### 2.3 보안

| ID | 요구사항 | 상세 |
|----|----------|------|
| GW-SEC-001 | CORS 처리 | 허용 Origin 환경별 설정, Credentials 허용 |
| GW-SEC-002 | 허용 Method | GET, POST, PUT, DELETE, OPTIONS |
| GW-SEC-003 | 허용 Header | Authorization, Content-Type, Idempotency-Key |
| GW-SEC-004 | JWT 사전 검증 | Keycloak JWKS 공개키로 토큰 서명/만료 검증, 권한 확인은 각 서비스에서 수행 |

---

## 3. Auth Service 요구사항

> 인증/인가는 **Keycloak(OIDC)**으로 처리한다. auth-service는 Keycloak Admin API를 활용한 회원가입, 프로필 보완 API를 제공한다.

### 3.1 Keycloak 설정

| ID | 요구사항 | 상세 |
|----|----------|------|
| AUTH-KC-001 | Realm 구성 | `locker-room` Realm 생성, 클라이언트/역할/IdP 설정 |
| AUTH-KC-002 | Client 등록 | 프론트엔드(Public, PKCE), auth-service(Confidential, Admin API용), ai-service(Confidential, Client Credentials) |
| AUTH-KC-003 | Realm Role | `user`, `admin` 역할 생성, 회원가입 시 기본 `user` 역할 부여 |
| AUTH-KC-004 | Identity Provider | Google, Kakao, Naver를 Keycloak IdP로 등록 |
| AUTH-KC-005 | SMTP 설정 | Keycloak Realm에 SMTP 설정, 비밀번호 재설정 이메일 자동 발송 |
| AUTH-KC-006 | 토큰 설정 | Access Token 30분, Refresh Token 7일, 슬라이딩 갱신 활성화 |

### 3.2 회원가입

| ID | 요구사항 | 상세 |
|----|----------|------|
| AUTH-REG-001 | 로컬 회원가입 | Keycloak Admin API로 사용자 생성 + 로컬 DB에 응원팀 저장 |
| AUTH-REG-002 | 이메일 유니크 | Keycloak + 로컬 DB 모두에서 이메일 중복 불가 |
| AUTH-REG-003 | 닉네임 유니크 | 로컬 DB에서 닉네임 중복 불가 |
| AUTH-REG-004 | 비밀번호 정책 | Keycloak Password Policy 설정 (8~20자, 영문+숫자+특수문자) |
| AUTH-REG-005 | 응원팀 필수 | 종목별 1팀 필수 선택, 가입 시점에 user_teams 동시 생성 |
| AUTH-REG-006 | keycloak_id 매핑 | 로컬 users 테이블에 Keycloak User ID(UUID) 저장 |
| AUTH-REG-007 | 보상 트랜잭션 | 로컬 DB 저장 실패 시 Keycloak 사용자 삭제 |

### 3.3 SSO 프로필 보완

| ID | 요구사항 | 상세 |
|----|----------|------|
| AUTH-SSO-001 | IdP 자동 생성 | Keycloak Identity Provider Brokering으로 SSO 사용자 자동 생성 |
| AUTH-SSO-002 | 프로필 감지 | 프론트엔드가 GET /users/me → 404 시 프로필 보완 필요로 판단 |
| AUTH-SSO-003 | 프로필 보완 | POST /auth/profile/complete로 닉네임, 응원팀 설정 |
| AUTH-SSO-004 | 중복 방지 | 이미 프로필이 완료된 사용자는 409 에러 |
| AUTH-SSO-005 | 지원 SSO Provider | Google, Kakao, Naver (Keycloak IdP) |

### 3.4 로그인/로그아웃/토큰 (Keycloak 직접 처리)

| ID | 요구사항 | 상세 |
|----|----------|------|
| AUTH-TK-001 | 로그인 | 프론트엔드 → Keycloak Authorization Code Flow + PKCE |
| AUTH-TK-002 | SSO 로그인 | 프론트엔드 → Keycloak IdP Brokering (kc_idp_hint 파라미터) |
| AUTH-TK-003 | 토큰 갱신 | 프론트엔드 → Keycloak Token Endpoint (grant_type=refresh_token) |
| AUTH-TK-004 | 로그아웃 | 프론트엔드 → Keycloak Logout Endpoint (세션 종료 + 토큰 무효화) |
| AUTH-TK-005 | 정지 계정 차단 | Gateway 또는 Resource Server에서 JWT 클레임 기반 검증 |
| AUTH-TK-006 | 비밀번호 재설정 | Keycloak 로그인 페이지 "Forgot Password" (SMTP로 자동 발송) |

### 3.5 토큰 검증 (각 서비스 공통)

| ID | 요구사항 | 상세 |
|----|----------|------|
| AUTH-VERIFY-001 | JWT 검증 | spring-boot-starter-oauth2-resource-server + Keycloak JWKS 공개키 |
| AUTH-VERIFY-002 | 역할 매핑 | JwtAuthConverter로 Keycloak `realm_access.roles` → Spring Security GrantedAuthority |
| AUTH-VERIFY-003 | issuer 검증 | JWT의 `iss` 클레임이 Keycloak Realm URL과 일치하는지 검증 |
| AUTH-VERIFY-004 | 만료 검증 | JWT의 `exp` 클레임으로 토큰 만료 자동 검증 |

### 3.6 서비스 간 인증 (Client Credentials)

| ID | 요구사항 | 상세 |
|----|----------|------|
| AUTH-S2S-001 | Client Credentials | ai-service → Keycloak Token Endpoint에서 토큰 발급 → resource-service API 호출 |
| AUTH-S2S-002 | 클라이언트 구분 | JWT의 `azp`(authorized party) 클레임으로 서비스 간 호출 판별 |
| AUTH-S2S-003 | 토큰 만료 | Keycloak Client 설정에서 서비스 간 토큰 1시간 만료 |

---

## 4. Resource Service 요구사항

### 4.1 사용자 관리

| ID | 요구사항 | 상세 |
|----|----------|------|
| RES-USER-001 | 내 정보 조회 | 인증된 사용자의 프로필 + 응원팀 목록 반환 |
| RES-USER-002 | 내 정보 수정 | 닉네임 변경, 비밀번호 변경 (현재 비밀번호 확인 필수) |
| RES-USER-003 | 응원팀 불변 | 응원팀 수정 API 제공하지 않음 (변경 불가 정책) |
| RES-USER-004 | 회원 탈퇴 | Soft Delete + user_withdrawals 이력 기록 |
| RES-USER-005 | 탈퇴 후 데이터 | 게시글/댓글 유지, 작성자 표시 "탈퇴한 사용자" |
| RES-USER-006 | 내 활동 조회 | 내가 쓴 글, 쓴 댓글, 좋아요한 글 목록 (Cursor 페이지네이션) |

### 4.2 종목/팀

| ID | 요구사항 | 상세 |
|----|----------|------|
| RES-SPORT-001 | 종목 목록 조회 | 활성화된 종목 목록, 인증 불필요 |
| RES-SPORT-002 | 팀 목록 조회 | 종목별 활성화된 팀 목록 (이름, 로고), 인증 불필요 |
| RES-SPORT-003 | 데이터 확장성 | 종목/팀 추가 시 코드 변경 없이 DB 데이터만 추가 |

### 4.3 게시판

| ID | 요구사항 | 상세 |
|----|----------|------|
| RES-BOARD-001 | 게시판 목록 | 인증 시 공통 + 본인 팀 전용, 비인증 시 공통만 |
| RES-BOARD-002 | 팀 전용 접근 제어 | TEAM 타입 게시판은 해당 팀 user_teams 존재 여부 검증 |
| RES-BOARD-003 | 게시판 유형 | TEAM, COMMON, QNA, NOTICE, NEWS |

### 4.4 게시글

| ID | 요구사항 | 상세 |
|----|----------|------|
| RES-POST-001 | 게시글 작성 | 게시판 접근 권한 검증 → 저장 → QNA 게시판이면 Kafka 이벤트 발행 |
| RES-POST-002 | 게시글 목록 | Cursor 기반 페이지네이션, 기본 20건, 최대 100건 |
| RES-POST-003 | 게시글 정렬 | createdAt(기본), likeCount (camelCase) |
| RES-POST-004 | 게시글 검색 | keyword + searchType (TITLE, CONTENT, TITLE_CONTENT, NICKNAME) |
| RES-POST-005 | 게시글 상세 | 조회 시 view_count +1, 인증 사용자는 isLiked 포함 |
| RES-POST-006 | 게시글 수정 | 작성자 본인만, 첨부파일 변경 시 제거된 파일 S3 삭제 |
| RES-POST-007 | 게시글 삭제 | 작성자 또는 관리자, Soft Delete, 첨부파일 S3 삭제 |
| RES-POST-008 | 좋아요 토글 | 좋아요/해제 토글 방식, like_count 동기 업데이트 |
| RES-POST-009 | 게시글 신고 | 동일 게시글 중복 신고 불가, 상태 PENDING으로 생성 |
| RES-POST-010 | 멱등성 | POST 요청 시 Idempotency-Key 헤더 필수, Redis 24시간 보관 |

### 4.5 댓글

| ID | 요구사항 | 상세 |
|----|----------|------|
| RES-CMT-001 | 댓글 목록 | Cursor 페이지네이션, 대댓글 중첩 구조로 반환 |
| RES-CMT-002 | 댓글 작성 | post의 comment_count +1, 게시글 작성자에게 알림 Kafka 발행 |
| RES-CMT-003 | 대댓글 작성 | depth 1단계 제한, parent_id가 이미 대댓글이면 거부 |
| RES-CMT-004 | 대댓글 알림 | 부모 댓글 작성자에게 알림 Kafka 발행 (본인 제외) |
| RES-CMT-005 | 댓글 수정 | 작성자 본인만 |
| RES-CMT-006 | 댓글 삭제 | Soft Delete, 대댓글 존재 시 "삭제된 댓글입니다." 표시 |

### 4.6 공지사항

| ID | 요구사항 | 상세 |
|----|----------|------|
| RES-NOTICE-001 | 공지 목록 | isPinned=true 우선 정렬, teamId 필터 가능 |
| RES-NOTICE-002 | 공지 상세 | 인증 불필요 |

### 4.7 고객센터

| ID | 요구사항 | 상세 |
|----|----------|------|
| RES-INQ-001 | 문의 작성 | type (GENERAL, BUG, SUGGESTION), 첨부파일 가능 |
| RES-INQ-002 | 내 문의 조회 | 본인 문의만 조회 가능, 답변 내역 포함 |

### 4.8 요청

| ID | 요구사항 | 상세 |
|----|----------|------|
| RES-REQ-001 | 추가 요청 | type (SPORT, TEAM), 요청 사유 필수 |
| RES-REQ-002 | 내 요청 조회 | 본인 요청만 조회, 처리 상태/반려 사유 포함 |

### 4.9 알림

| ID | 요구사항 | 상세 |
|----|----------|------|
| RES-NOTI-001 | 알림 목록 | Cursor 페이지네이션, 최신순 |
| RES-NOTI-002 | 미읽음 수 | is_read=false인 알림 카운트 |
| RES-NOTI-003 | 읽음 처리 | 개별 읽음 + 전체 읽음 |
| RES-NOTI-004 | 알림 유형 | COMMENT, REPLY, NOTICE, INQUIRY_REPLY |

### 4.10 파일

| ID | 요구사항 | 상세 |
|----|----------|------|
| RES-FILE-001 | 파일 업로드 | S3 저장, DB에 메타데이터 기록 |
| RES-FILE-002 | 이미지 제한 | 최대 10MB, jpeg/png/gif/webp |
| RES-FILE-003 | 일반 파일 제한 | 최대 20MB, pdf/txt |
| RES-FILE-004 | 첨부 개수 | 건당 최대 5개 |
| RES-FILE-005 | MIME 검증 | Content-Type 헤더 + 파일 시그니처(매직넘버) 이중 검증 |
| RES-FILE-006 | 파일 삭제 | DB Soft Delete가 아닌 물리 삭제 (S3 + DB) |
| RES-FILE-007 | 파일명 저장 | 원본명(original_name) 보관, 저장명(stored_name)은 UUID 기반 |

### 4.11 관리자

| ID | 요구사항 | 상세 |
|----|----------|------|
| RES-ADM-001 | 권한 검증 | 모든 관리자 API는 role=ADMIN 검증 필수 |
| RES-ADM-002 | 회원 목록 | 검색 (닉네임, 이메일), 역할 필터 |
| RES-ADM-003 | 회원 정지 | 정지 사유 + 기간 설정, user_suspensions 기록 |
| RES-ADM-004 | 신고 처리 | APPROVED/REJECTED 처리, 승인 시 게시글 삭제 또는 작성자 정지 |
| RES-ADM-005 | 신고 처리 알림 | 처리 완료 시 notification.report-processed Kafka 이벤트 |
| RES-ADM-006 | 공지사항 관리 | 작성/수정/삭제, 상단 고정, 노출 범위 (ALL/TEAM) |
| RES-ADM-007 | 문의 답변 | 답변 작성 → status ANSWERED 변경 → notification.inquiry-replied Kafka 이벤트 |
| RES-ADM-008 | 요청 처리 | APPROVED 시 종목/팀 + 게시판(TEAM, NEWS) 자동 생성 |

### 4.12 Kafka Producer 요구사항

| ID | 토픽 | 트리거 | Key |
|----|------|--------|-----|
| RES-KF-001 | `qna-post.created` | Q&A 게시글 작성 | postId |
| RES-KF-002 | `notification.comment` | 댓글 작성 (작성자 ≠ 게시글 작성자) | userId (수신자) |
| RES-KF-003 | `notification.reply` | 대댓글 작성 (작성자 ≠ 부모 댓글 작성자) | userId (수신자) |
| RES-KF-004 | `notification.inquiry-replied` | 문의 답변 작성 | userId (문의자) |
| RES-KF-005 | `notification.report-processed` | 신고 처리 완료 | userId (신고자) |

### 4.13 멱등성 요구사항

| ID | 요구사항 | 상세 |
|----|----------|------|
| RES-IDEM-001 | 적용 대상 | 모든 POST 요청 (게시글, 댓글, 좋아요, 신고, 문의, 요청) |
| RES-IDEM-002 | 전달 방식 | HTTP 헤더 `Idempotency-Key: {UUID}` |
| RES-IDEM-003 | 저장 | Redis, key: `idempotency:{key}`, value: 응답 JSON |
| RES-IDEM-004 | TTL | 24시간 |
| RES-IDEM-005 | 중복 처리 | 동일 키 재요청 시 저장된 응답 반환, 비즈니스 로직 재실행하지 않음 |

---

## 5. AI Service 요구사항

> **TBD**: LLM 모델 미결정

### 5.1 Q&A 자동 답변

| ID | 요구사항 | 상세 |
|----|----------|------|
| AI-QNA-001 | 트리거 | Kafka `qna-post.created` 이벤트 consume |
| AI-QNA-002 | 처리 | 게시글 제목+내용을 LLM에 전달 → 답변 생성 |
| AI-QNA-003 | 답변 저장 | Client Credentials로 resource-service 댓글 작성 API 호출 |
| AI-QNA-004 | AI 표시 | 댓글 is_ai_generated=true로 저장 |
| AI-QNA-005 | 실패 처리 | 3회 재시도 (5s/30s/60s), 실패 시 DLQ (`qna-post.created.dlq`) |
| AI-QNA-006 | Consumer Group | `ai-consumer-group` |

### 5.2 팀 뉴스 자동 생성

| ID | 요구사항 | 상세 |
|----|----------|------|
| AI-NEWS-001 | 실행 주기 | 매일 오전 7시 (cron 스케줄러) |
| AI-NEWS-002 | 대상 | 활성화된 모든 팀 (is_active=true) |
| AI-NEWS-003 | 생성 | 팀명+종목 정보를 LLM에 전달 → 뉴스 콘텐츠 생성 |
| AI-NEWS-004 | 게시 | Client Credentials로 resource-service 게시글 작성 API 호출, NEWS 게시판에 등록 |
| AI-NEWS-005 | AI 표시 | 게시글 is_ai_generated=true로 저장 |
| AI-NEWS-006 | 부분 실패 허용 | 개별 팀 뉴스 생성 실패 시 로그 기록 후 나머지 팀 계속 처리 |

### 5.3 LLM 연동

| ID | 요구사항 | 상세 |
|----|----------|------|
| AI-LLM-001 | 타임아웃 | LLM API 호출 타임아웃 30초 |
| AI-LLM-002 | 프롬프트 관리 | 외부 설정 또는 DB 기반 프롬프트 템플릿 관리 |
| AI-LLM-003 | 응답 길이 | 뉴스: 최대 2000자, Q&A 답변: 최대 1000자 |
| AI-LLM-004 | 콘텐츠 필터링 | LLM 응답에 부적절한 내용 포함 여부 사후 검증 |

---

## 6. Notification Service 요구사항

### 6.1 Kafka Consumer

| ID | 요구사항 | 상세 |
|----|----------|------|
| NOTI-KF-001 | 구독 토픽 | `notification.*` |
| NOTI-KF-002 | Consumer Group | `notification-consumer-group` |
| NOTI-KF-003 | 파티셔닝 | `notification.*`: userId 기반 (동일 사용자 순서 보장) |

### 6.2 인앱 알림

| ID | 요구사항 | 상세 |
|----|----------|------|
| NOTI-APP-001 | 알림 저장 | notifications 테이블에 저장 (resource-service DB 직접 접근 또는 REST 호출) |
| NOTI-APP-002 | 알림 메시지 생성 | 이벤트 유형별 메시지 템플릿 기반 생성 |
| NOTI-APP-003 | 발신자 정보 | 알림 메시지에 발신자 닉네임 포함 |

### 6.3 이메일 발송

| ID | 요구사항 | 상세 |
|----|----------|------|
| NOTI-EMAIL-001 | 비밀번호 재설정 | Keycloak이 SMTP로 직접 발송 (notification-service 미관여) |
| NOTI-EMAIL-002 | 이메일 템플릿 | Keycloak Realm 이메일 템플릿 설정 |
| NOTI-EMAIL-003 | 발신 주소 | Keycloak SMTP 설정에서 `noreply@lockerroom.com` 지정 |

### 6.4 재시도 / DLQ

| ID | 요구사항 | 상세 |
|----|----------|------|
| NOTI-DLQ-001 | 알림 재시도 | 3회 (5s / 30s / 60s) |
| NOTI-DLQ-002 | DLQ 토픽 | `notification.dlq` |
| NOTI-DLQ-004 | DLQ 재처리 | 관리자 대시보드 수동 재처리 또는 스케줄러 일괄 재처리 |

---

## 7. 데이터 요구사항

### 7.1 데이터베이스

| ID | 요구사항 | 상세 |
|----|----------|------|
| DATA-DB-001 | DBMS | MariaDB (InnoDB), JDBC Driver: MariaDB Connector/J |
| DATA-DB-002 | 문자셋 | utf8mb4 / utf8mb4_unicode_ci |
| DATA-DB-003 | PK 전략 | BIGINT AUTO_INCREMENT |
| DATA-DB-004 | 시간 저장 | DATETIME (UTC 기준) |
| DATA-DB-005 | Soft Delete | 공통 필드 `deleted_at` (NULL = 활성, NOT NULL = 삭제) |
| DATA-DB-006 | 감사 필드 | `created_at`, `updated_at` 모든 테이블에 필수 |

### 7.2 인덱스

| ID | 테이블 | 인덱스 | 용도 |
|----|--------|--------|------|
| DATA-IDX-001 | users | uk_users_email (UNIQUE) | 이메일 중복 검증, 로그인 조회 |
| DATA-IDX-002 | user_teams | uk_user_teams_user_sport (UNIQUE) | 종목당 1팀 제약 |
| DATA-IDX-003 | posts | idx_posts_board_created | 게시판별 최신글 목록 |
| DATA-IDX-004 | posts | idx_posts_user | 내가 쓴 글 조회 |
| DATA-IDX-005 | comments | idx_comments_post_created | 게시글별 댓글 목록 |
| DATA-IDX-006 | comments | idx_comments_user | 내가 쓴 댓글 조회 |
| DATA-IDX-007 | post_likes | uk_post_likes_post_user (UNIQUE) | 좋아요 중복 방지 |
| DATA-IDX-008 | post_reports | idx_post_reports_status | 미처리 신고 필터링 |
| DATA-IDX-009 | notifications | idx_notifications_user_read | 읽지 않은 알림 조회 |
| DATA-IDX-010 | files | idx_files_target | 대상별 첨부파일 조회 |

### 7.3 Redis 데이터

| Key 패턴 | Value | TTL | 용도 |
|----------|-------|-----|------|
| `idempotency:{key}` | 응답 JSON | 24시간 | 멱등성 보장 |
| `rate-limit:{key}` | 요청 카운트 | 1분 | Rate Limiting |

> 토큰 관리(발급, 갱신, 블랙리스트), 비밀번호 재설정, SSO 임시 토큰은 Keycloak이 자체 관리하므로 Redis에 저장하지 않는다.

### 7.4 데이터 보존 정책

| 데이터 | 보존 기간 | 처리 |
|--------|----------|------|
| 사용자 (탈퇴) | Soft Delete 후 30일 | 물리 삭제 또는 익명화 |
| 게시글/댓글 | 영구 | Soft Delete |
| 파일 | 대상 삭제 시 동시 삭제 | S3 + DB 물리 삭제 |
| 알림 | 90일 | 스케줄러로 자동 삭제 |
| 로그 | 90일 | S3 보관 후 삭제 |

---

## 8. 공통 API 요구사항

### 8.1 요청/응답

| ID | 요구사항 | 상세 |
|----|----------|------|
| API-COMMON-001 | 통신 형식 | JSON (application/json) |
| API-COMMON-002 | 파일 업로드 형식 | multipart/form-data |
| API-COMMON-003 | 문자 인코딩 | UTF-8 |
| API-COMMON-004 | 날짜/시간 형식 | ISO 8601 (UTC), 예: `2026-02-15T09:00:00Z` |
| API-COMMON-005 | 응답 구조 | `{ code, message, data }` 통일 |
| API-COMMON-006 | 에러 코드 형식 | `{도메인}_{에러유형}` (UPPER_SNAKE_CASE) |

### 8.2 페이지네이션

| ID | 요구사항 | 상세 |
|----|----------|------|
| API-PAGE-001 | 방식 | Cursor 기반 (Offset 사용 금지) |
| API-PAGE-002 | 기본 크기 | 20건 |
| API-PAGE-003 | 최대 크기 | 100건 |
| API-PAGE-004 | 응답 | `{ items, nextCursor, hasNext }` |
| API-PAGE-005 | Cursor 인코딩 | Base64 |

### 8.3 유효성 검증

| ID | 요구사항 | 상세 |
|----|----------|------|
| API-VALID-001 | 검증 위치 | Controller 레이어 (@Valid) |
| API-VALID-002 | 검증 실패 응답 | 400 + 필드별 에러 메시지 목록 |
| API-VALID-003 | 메시지 i18n | MessageSource 기반 다국어 검증 메시지 |

---

## 9. 보안 요구사항

| ID | 요구사항 | 상세 |
|----|----------|------|
| SEC-001 | HTTPS 필수 | 모든 환경에서 TLS 1.2+ 적용 (로컬 제외) |
| SEC-002 | 비밀번호 해시 | Keycloak Password Policy에서 관리 (BCrypt) |
| SEC-003 | SQL Injection | JPA Parameterized Query 사용, Native Query 시 파라미터 바인딩 필수 |
| SEC-004 | XSS 방지 | 입출력 데이터 이스케이핑, HTML 태그 필터링 |
| SEC-005 | CSRF | JWT 기반 Stateless이므로 CSRF 토큰 불필요, CORS로 제어 |
| SEC-006 | 민감 정보 로깅 금지 | 비밀번호, 토큰 등 로그에 남기지 않음 |
| SEC-007 | 환경 변수 | Keycloak Client Secret, DB Password, AWS Credentials 등 환경 변수로 관리 (.env, application.yml 외부화) |
| SEC-008 | 의존성 보안 | 정기적 의존성 취약점 스캔 (Dependabot, OWASP Dependency Check) |

---

## 10. 성능 요구사항

| ID | 요구사항 | 기준 |
|----|----------|------|
| PERF-001 | API 응답 시간 | 95th percentile ≤ 500ms |
| PERF-002 | DB 쿼리 | 단일 쿼리 ≤ 100ms |
| PERF-003 | 동시 접속 | 최소 1,000 동시 사용자 |
| PERF-004 | Kafka 처리 지연 | 이벤트 발행 → Consumer 처리 ≤ 5초 (정상 시) |
| PERF-005 | 파일 업로드 | 20MB 파일 업로드 ≤ 10초 |
| PERF-006 | Redis 조회 | 단일 조회 ≤ 5ms |

---

## 11. 가용성 / 안정성 요구사항

| ID | 요구사항 | 기준 |
|----|----------|------|
| AVAIL-001 | 서비스 가용률 | 99.5% (월간) |
| AVAIL-002 | 서비스 격리 | 개별 서비스 장애가 전체 시스템에 전파되지 않음 |
| AVAIL-003 | Kafka 장애 대응 | Kafka 장애 시에도 동기 API는 정상 동작 (알림만 지연) |
| AVAIL-004 | Redis 장애 대응 | Redis 장애 시 Rate Limiting/멱등성 비활성화, 인증은 Keycloak JWKS 기반 JWT 검증으로 독립 동작 |
| AVAIL-005 | 장애 복구 | RTO ≤ 1시간, RPO ≤ 1시간 |
| AVAIL-006 | DB 백업 | 일별 전체 백업 + 실시간 바이너리 로그 |
| AVAIL-007 | 무중단 배포 | Rolling Deployment 적용 |

---

## 12. 로깅 / 모니터링 요구사항

### 12.1 로깅

| ID | 요구사항 | 상세 |
|----|----------|------|
| LOG-001 | 요청/응답 로그 | Method, URI, 상태 코드, 응답 시간 (Filter/Interceptor) |
| LOG-002 | 비즈니스 로그 | 주요 이벤트 (게시글 작성, 좋아요, 신고 등) |
| LOG-003 | 에러 로그 | 스택 트레이스 + 요청 정보 |
| LOG-004 | Kafka 로그 | 토픽, 파티션, 오프셋, 처리 결과 |
| LOG-005 | 로그 포맷 | 구조화 로깅, traceId 포함 |
| LOG-006 | 민감 정보 마스킹 | 비밀번호, 토큰, 개인정보 마스킹 처리 |
| LOG-007 | 로그 보관 | 로컬: 파일 (logs/), 운영: AWS S3 (90일 보관) |

### 12.2 모니터링

| ID | 요구사항 | 상세 |
|----|----------|------|
| MON-001 | 헬스체크 | Spring Boot Actuator `/actuator/health` — DB, Redis, Kafka, Disk 상태 자동 감지 |
| MON-002 | 메트릭 수집 | Actuator `/actuator/metrics` 기본 제공 (JVM, HTTP, 커넥션 풀). 향후 Prometheus + Grafana 또는 AWS CloudWatch 연동 |
| MON-003 | 분산 트레이싱 | Micrometer Tracing + Zipkin/Jaeger (향후 적용) |
| MON-004 | 중앙화 로깅 | ELK Stack 또는 AWS CloudWatch Logs (향후 적용) |
| MON-005 | 런타임 로그 제어 | Actuator `/actuator/loggers` — 서비스 재시작 없이 로그 레벨 변경 |
| MON-006 | 알림 | 서비스 다운, 에러율 급증 시 Slack/이메일 알림 (향후 적용) |

---

## 13. 테스트 요구사항

| ID | 요구사항 | 상세 |
|----|----------|------|
| TEST-001 | Unit Test | Service, Util 계층, JUnit 5 + Mockito |
| TEST-002 | Integration Test | Repository 계층, @DataJpaTest + H2 |
| TEST-003 | Controller Test | @WebMvcTest + MockMvc, 요청/응답 검증 |
| TEST-004 | E2E Test | @SpringBootTest + TestRestTemplate, 전체 API 흐름 |
| TEST-005 | 커버리지 | Service 80%, Controller 70%, Repository 60% |
| TEST-006 | 네이밍 | `{메서드명}_{시나리오}_{기대결과}` |
| TEST-007 | Kafka 테스트 | EmbeddedKafka 또는 Testcontainers 활용 |

---

## 14. 배포 / 인프라 요구사항

| ID | 요구사항 | 상세 |
|----|----------|------|
| INFRA-001 | 컨테이너화 | 서비스별 Docker 이미지 빌드 |
| INFRA-002 | 이미지 레지스트리 | AWS ECR |
| INFRA-003 | 배포 환경 | AWS EC2 |
| INFRA-004 | CI/CD | Jenkins 파이프라인 (Build → Test → Docker → Push → Deploy) |
| INFRA-005 | 환경 분리 | local / dev / staging / prod (Spring Profiles) |
| INFRA-006 | 무중단 배포 | Rolling Deployment |
| INFRA-007 | 설정 관리 | 환경 변수 기반 (application-{profile}.yml), 민감 정보는 AWS Parameter Store 또는 Secrets Manager 검토 |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-15 | - | 초안 작성 |
| 1.1 | 2026-02-15 | - | MariaDB Connector/J 드라이버 명시 |
| 1.2 | 2026-02-16 | - | 인증 서버 Keycloak 확정. Auth Service 요구사항 재구성, Redis 토큰 저장 제거, email.password-reset Kafka 토픽 제거, Gateway 블랙리스트 제거 |
| 1.3 | 2026-02-25 | - | 모니터링 섹션 TBD 해소. Spring Boot Actuator 기반 헬스체크/메트릭/로그 제어 요구사항 확정 (MON-001~006) |
| 1.4 | 2026-02-25 | - | Phase 20 반영: 게시글 정렬 파라미터 camelCase 통일 (created_at→createdAt, like_count→likeCount) |
