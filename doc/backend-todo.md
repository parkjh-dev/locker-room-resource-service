# Locker Room — 백엔드 신규/변경 API 정리

> 프론트엔드 (`locker-room-web-service`) 최근 작업분에서 호출하지만 백엔드 (`locker-room-resource-service`) 에 아직 구현되지 않은 엔드포인트 + 기존 엔드포인트의 페이로드/응답 변경 사항.
>
> 모든 경로는 prefix `/api/v1` 기준. 인증은 OAuth2 Resource Server (Keycloak JWT) 따른다.

---

## 진행 상태 (2026-04-30 기준)

| 섹션 | 상태 | 비고 |
|---|---|---|
| §1 인증 흐름 | 🔵 **out-of-scope** | Keycloak이 회원가입·이메일·휴대폰 인증을 모두 처리하는 방향 검토 중. 리소스 서버 책임 외 |
| §2 사용자 (확장 + 응원팀 등록 + 온보딩 skip) | ✅ **완료** | Phase 2~3 |
| §3 게시글 카테고리 + 투표 | ✅ **완료** | Phase 4. category는 Post/FootballPost/BaseballPost 모두 추가, Poll은 Post에만 |
| §4 팀 게시판 대시보드 | 🟡 **stub 완료, 데이터 미구현** | endpoint·DTO·Controller 모두 작성. nextMatch=null, recentMatches=[], standing=null로 응답. 실데이터는 Match·Standing 도메인 신설 후 |
| §5 팀 통계 | ✅ **완료** | Phase 5 (Redis 캐시 1시간) |
| §6 종목 cascading | ✅ **완료** | Phase 1 (농구·배구는 entity 부재 → 빈 배열) |
| §7 권한 정책 | ✅ **이미 구현** | InquiryController/RequestController가 @CurrentUserId 기반으로 본인 한정 |

### §4 후속 작업 (별도 phase 필요)

- [ ] `Match` entity 신설 (경기 일정·결과)
- [ ] `Standing` entity 신설 (시즌 순위 캐시 또는 실시간 집계 정책 결정)
- [ ] 데이터 소스 결정: K리그/KBO 공식 OPI 연동? 운영 어드민 입력? 외부 스포츠 데이터 API?
- [ ] 시즌 메타데이터 모델링 (시즌 시작·종료, 종목별 표기 포맷)
- [ ] `TeamProfileResponse.founded`/`venue`/`description` 컬럼 추가 또는 별도 메타 테이블
- [ ] `TeamServiceImpl`의 TODO 주석 따라 stub → 실데이터 교체

---

## 1. 회원가입 / 인증 흐름

### 1.1 회원가입 페이로드 변경 — `POST /auth/signup`

**변경 사항**
- `phone` 필수 추가 (휴대폰 인증 완료한 번호)
- `teams` 제거 — 응원팀 등록은 가입 후 별도 온보딩 단계로 분리됨

**Request**
```json
{
  "email": "user@example.com",
  "password": "string",
  "phone": "01012345678",
  "nickname": "야구사랑러"
}
```

**Response**: 기존 그대로 `{ id }`

**비고**
- 백엔드는 가입 직후 자동으로 인증 메일 발송 (이메일 soft gate)
- 휴대폰 번호는 `users.phone` 컬럼에 저장 (정규식 `^01[0-9]{8,9}$`)
- 응원팀 미등록 상태로 가입 완료 → `onboardingCompletedAt = null` 초기값

---

### 1.2 SSO 프로필 보완 페이로드 변경 — `POST /auth/profile/complete`

**변경 사항**
- `teams` 제거 — 응원팀 등록은 별도 온보딩 단계

**Request**
```json
{ "nickname": "야구사랑러" }
```

---

### 1.3 휴대폰 인증번호 발송 — `POST /auth/phone/verification` (신규)

**Request**
```json
{ "phone": "01012345678" }
```

**Response**
```json
{ "expiresInSec": 180 }
```

**비고**
- 인증 불필요 (가입 전 호출)
- 발송 실패/차단 정책: 같은 번호로 1분 내 재발송 차단, 1일 5회 제한 등 (정책 협의)
- SMS 게이트웨이 연동 필요

---

### 1.4 휴대폰 인증번호 검증 — `POST /auth/phone/verification/confirm` (신규)

**Request**
```json
{ "phone": "01012345678", "code": "123456" }
```

**Response (성공)**: `{ "verified": true }`
**Response (실패, 400)**: `code: PHONE_VERIFICATION_INVALID`

**비고**
- 검증 통과 시 한정 시간(예: 5분) 동안 해당 번호를 "검증됨" 상태로 보관 → signup 시 동일 번호 검증
- 인증 불필요 (가입 전)

---

### 1.5 이메일 인증 메일 재발송 — `POST /auth/email/verification/resend` (신규)

**Request (옵션)**
```json
{ "email": "user@example.com" }   // 미로그인 사용자(가입 직후)가 호출 시
```
또는 **빈 바디** (로그인 사용자가 본인 이메일로 재발송 시 — 토큰에서 식별)

**Response**: `null`

**Rate limit 권장**: 1분 내 1회

---

### 1.6 이메일 인증 토큰 검증 — `POST /auth/email/verification/confirm` (신규)

**Request**
```json
{ "token": "..." }
```

**Response (성공)**: `{ "verified": true }`
**Response (실패, 400)**: `code: EMAIL_VERIFICATION_INVALID`

**비고**
- 토큰 만료 24시간 권장
- 검증 성공 시 `users.email_verified = true` 업데이트

---

## 2. 사용자 (`/users/me`) 응답·페이로드 확장

### 2.1 `GET /users/me` 응답 필드 추가

기존 `UserResponse`에 다음 필드 추가:

```diff
{
  "id": 42,
  "email": "user@example.com",
+ "emailVerified": false,
+ "phone": "01012345678",
  "nickname": "야구사랑러",
  "role": "USER",
  "provider": "LOCAL",
  "profileImageUrl": null,
  "teams": [...],
+ "onboardingCompletedAt": null,
  "createdAt": "2026-01-15T09:30:00"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `emailVerified` | boolean | 이메일 인증 여부. 미인증 시 글쓰기/댓글 차단(soft gate) |
| `phone` | string | 휴대폰 번호 (가입 시 인증된 번호) |
| `onboardingCompletedAt` | string \| null | 응원팀 등록 또는 명시적 skip 시각. null이면 첫 로그인으로 판정 → `/onboarding/teams`로 안내 |

DB 마이그레이션:
```sql
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
ALTER TABLE users ADD COLUMN onboarding_completed_at TIMESTAMP NULL;
```

---

### 2.2 `PUT /users/me` 페이로드 확장

기존 `UserUpdateRequest`(닉네임 + 비밀번호) 에 `profileImageUrl` 추가:

```diff
{
  "nickname": "...",
  "currentPassword": "...",
  "newPassword": "...",
+ "profileImageUrl": "https://cdn.example.com/profiles/42.jpg"   // null 보내면 제거
}
```

**비고**
- 사진 업로드는 기존 `POST /files` 사용 후 받은 URL을 여기로 보냄
- `profileImageUrl: null` 명시 전송 → 사진 제거
- 필드 미전송 → 기존 사진 유지

---

### 2.3 응원팀 등록 — `POST /users/me/teams` (신규)

**Request**
```json
{
  "teams": [
    { "sportId": 1, "teamId": 101 },
    { "sportId": 2, "teamId": 201 }
  ]
}
```

**Response**: 갱신된 `UserResponse` 전체

**핵심 정책**
- **종목별 락**: 이미 등록한 종목의 팀은 변경 불가 (해당 항목은 무시 or 400)
- **미등록 종목은 추후 추가 가능** — 첫 등록 시 축구만 골랐다가 6개월 후 야구 추가하는 케이스 정상 흐름
- 호출 성공 시 백엔드가 `onboardingCompletedAt`을 `now()`로 자동 셋 (idempotent — null일 때만)

**인증**: 필수

---

### 2.4 온보딩 건너뛰기 — `POST /users/me/onboarding/skip` (신규)

**Request**: 빈 바디
**Response**: 갱신된 `UserResponse`

**비고**
- 응원팀 등록 안 하고 자유게시판만 사용하려는 사용자용
- `onboardingCompletedAt`만 `now()`로 셋, `teams`는 빈 상태 유지
- 이미 셋된 사용자가 호출해도 idempotent

---

## 3. 게시글 (`/posts`) 메타 확장 — 말머리 + 투표

### 3.1 `POST /posts` 페이로드 확장

```diff
{
  "boardId": 1,
  "title": "...",
  "content": "...",
+ "category": "REVIEW",
+ "poll": {
+   "question": "오늘 MVP는?",   // null 가능
+   "options": ["손흥민", "황희찬", "이강인"],
+   "expiresAt": "2026-05-02T19:00:00"
+ },
  "fileIds": [101, 102]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `category` | enum 필수 | `GENERAL` / `REVIEW` / `PREDICTION` / `QUESTION` / `MEME` / `NEWS` |
| `poll` | object \| null | 투표 페이로드 (선택). `options` 2~5개, 각 50자 이하, 중복 불가 |

**비고**
- 본문 `content`는 Tiptap이 만든 **HTML 문자열** (백엔드는 sanitize 후 저장 권장 — `<script>`, `on*` 속성 제거)
- 인라인 이미지가 본문 HTML 안 `<img src=...>`에 포함됨 → `fileIds`는 향후 deprecated 가능성 (백엔드와 협의 필요)

---

### 3.2 `PUT /posts/{postId}` 페이로드 확장

```diff
{
  "title": "...",
  "content": "...",
+ "category": "REVIEW",
  "fileIds": [...]
}
```

- **수정 시 투표(`poll`)는 변경 불가** — 참여자의 기존 표 보호
- `category`는 변경 가능

---

### 3.3 `GET /posts/{postId}` 응답 확장

```diff
{
  "id": 1024,
  "boardId": 1,
  "boardName": "...",
  "author": {...},
  "title": "...",
  "content": "...",
+ "category": "REVIEW",
+ "poll": {
+   "question": "오늘 MVP는?",
+   "options": [
+     { "id": 1, "text": "손흥민", "voteCount": 45 },
+     { "id": 2, "text": "황희찬", "voteCount": 28 },
+     { "id": 3, "text": "이강인", "voteCount": 17 }
+   ],
+   "expiresAt": "2026-05-02T19:00:00",
+   "totalVotes": 90,
+   "myVoteOptionId": 1   // 미투표면 null
+ },
  "viewCount": 1234,
  ...
}
```

`poll` 은 `null` 가능 (투표 없는 글).

---

### 3.4 `GET /boards/{boardId}/posts` 응답 확장 (목록)

```diff
{
  "id": 1024,
  "title": "...",
  "authorNickname": "...",
+ "category": "REVIEW",
+ "hasPoll": true,
  "viewCount": 1234,
  ...
}
```

- 목록에선 투표 상세 데이터 불필요 → `hasPoll: boolean` 만 노출 (목록 카드의 📊 아이콘 표시용)
- `GET /posts/popular` 도 동일하게 확장

---

### 3.5 게시글 투표 — `POST /posts/{postId}/vote` (신규)

**Request**
```json
{ "optionId": 2 }
```

**Response**: 갱신된 `Poll` 전체 (위 3.3의 `poll` 객체)

**정책**
- **한 번 투표하면 변경 불가** (idempotent — 이미 투표한 사용자가 다시 호출해도 200 + 현재 상태 반환)
- 마감(`expiresAt` 지남) 후 호출 시 400 (`POLL_EXPIRED`)
- 존재하지 않는 옵션 ID → 400 (`POLL_OPTION_INVALID`)

**인증**: 필수

---

## 4. 팀 게시판 대시보드

### 4.1 팀 대시보드 — `GET /teams/{teamId}/dashboard` (신규)

팀 게시판(`board.type = 'TEAM'`) 진입 시 헤더에 노출되는 통합 응답. round-trip 1번에 모든 정보.

**Response**
```json
{
  "team": {
    "id": 101,
    "name": "전북 현대 모터스",
    "logoUrl": null,
    "leagueName": "K리그1",
    "founded": 1994,
    "venue": "전주월드컵경기장",
    "description": "K리그1의 대표 명문 클럽. ..."
  },
  "season": "2026",
  "nextMatch": {
    "id": 9001,
    "competitionName": "K리그1 14R",
    "opponent": { "id": 102, "name": "울산 HD FC", "logoUrl": null },
    "isHome": true,
    "venue": "전주월드컵경기장",
    "kickoffAt": "2026-05-02T19:00:00"
  },
  "recentMatches": [
    {
      "id": 8001,
      "competitionName": "K리그1",
      "opponent": { "id": 105, "name": "포항 스틸러스", "logoUrl": null },
      "isHome": false,
      "teamScore": 2,
      "opponentScore": 1,
      "result": "WIN",
      "playedAt": "2026-04-25T19:00:00"
    }
    /* ... 최대 5개, 최신순 */
  ],
  "standing": {
    "rank": 3,
    "totalTeams": 12,
    "matchesPlayed": 13,
    "wins": 7,
    "draws": 4,
    "losses": 2,
    "points": 25,
    "goalsFor": 22,
    "goalsAgainst": 11,
    "goalDifference": 11
  }
}
```

**필드 비고**
| 필드 | 타입 | 설명 |
|---|---|---|
| `season` | string | 종목/리그별 자유 포맷 (`'2026'`, `'2025-26'` 등) |
| `nextMatch` | object \| null | 예정 경기 없으면 null |
| `recentMatches` | array | 최대 5개. result는 `WIN`/`DRAW`/`LOSS` |
| `standing` | object \| null | 시즌 초/플레이오프 등 순위 미정 시기엔 null |

**인증**: 불필요 (공개 조회)

**캐시**: 5분 권장 (분 단위로 안 바뀜)

**데이터 출처**: 외부 스포츠 데이터 API 연동 또는 운영 입력 (KBO/K리그/KBL 등 공식 OPI 활용 가능)

---

## 5. 메인 페이지 — 팀 통계

### 5.1 팀 랭킹 — `GET /stats/teams/ranking` (신규)

**Query Parameters**
| 파라미터 | 값 | 기본 |
|---|---|---|
| `metric` | `FOLLOWERS` (응원자 수) \| `AVG_POSTS` (일평균 게시글) | `FOLLOWERS` |
| `sport` | `ALL` \| `축구` \| `야구` \| `농구` \| `배구` | `ALL` |
| `size` | 정수 (1~10) | `3` |

**Response**
```json
[
  {
    "rank": 1,
    "team": { "id": 101, "name": "전북 현대 모터스", "logoUrl": null },
    "sportName": "축구",
    "followerCount": 12400,
    "avgPostsPerDay": 23
  },
  { "rank": 2, ... },
  { "rank": 3, ... }
]
```

**비고**
- 메인 페이지에 **시상대(podium)** 형태로 Top 3 노출용
- `metric` 기준으로 내림차순 정렬, `rank`는 백엔드가 부여
- 두 지표(`followerCount`, `avgPostsPerDay`) 모두 응답에 포함 → 프론트가 어느 metric으로 정렬됐든 두 값 다 표시 가능
- **계산 단위**: `avgPostsPerDay`는 최근 30일(또는 14일) 기준 일평균 게시글 수 권장 (정책 협의)

**인증**: 불필요 (공개 통계)

**캐시**: 통계라 5분 staleTime + 백엔드도 적극 캐싱 (Redis 권장 — 1시간 단위 갱신 등)

---

## 6. 종목/국가/리그/팀 4단계 Cascading (신규)

회원가입·온보딩의 응원팀 선택, 마이페이지의 종목별 추가에서 호출. 백엔드에 현재 `GET /sports`만 존재하므로 cascading 4-step 추가 필요.

### 6.1 종목별 국가 — `GET /sports/{sportId}/countries`

해당 종목의 리그가 운영되는 국가만 반환.

**Response**
```json
[
  { "id": 1, "nameKo": "대한민국", "code": "KR", "continentId": 1 },
  { "id": 2, "nameKo": "일본", "code": "JP", "continentId": 1 }
]
```

### 6.2 국가별 리그 — `GET /sports/{sportId}/countries/{countryId}/leagues`

```json
[
  { "id": 1, "nameKo": "K리그1", "sportId": 1, "countryId": 1, "tier": 1 }
]
```

### 6.3 리그별 팀 — `GET /leagues/{leagueId}/teams`

```json
[
  { "id": 101, "name": "전북 현대 모터스", "logoUrl": null, "isActive": true, "leagueId": 1 }
]
```

### 6.4 (옵션) 대륙 — `GET /continents`

```json
[ { "id": 1, "nameKo": "아시아", "code": "AS" } ]
```

**모두 인증 불필요** (공개 데이터).

---

## 7. (참고) 권한 정책 — 1:1 문의 / 종목·구단 요청

프론트는 `/inquiries`, `/requests` 목록을 통합 페이지(`/support`) 의 두 탭에서 노출. **현재 백엔드 응답은 전체 목록을 그대로 반환하므로 권한 분리가 필요**.

| 엔드포인트 | 일반 사용자 응답 | 관리자 응답 |
|---|---|---|
| `GET /inquiries` | **본인이 작성한 문의만** | 전체 |
| `GET /inquiries/{id}` | 본인 것만 (그 외 403) | 전체 |
| `GET /requests` | **본인이 작성한 요청만** | 전체 |
| `GET /requests/{id}` | 본인 것만 (그 외 403) | 전체 |

(관리자 콘솔에는 별도 `/admin/inquiries`, `/admin/requests`가 있어 관리자 전체 조회는 그쪽으로 일원화하는 것도 옵션)

---

## 8. 작업 우선순위 제안

| Phase | 작업 | 영향도 |
|---|---|---|
| **1순위 — 데이터 모델 변경** | `users` 테이블 컬럼 추가 (emailVerified / phone / onboardingCompletedAt), `posts` 테이블 (category / poll 관련 테이블 신규), `UserResponse`/`PostDetailResponse`/`PostListResponse` 확장 | 다수 화면 즉시 동작 |
| **2순위 — 인증 흐름** | 1.1~1.6 (휴대폰·이메일 인증) | 가입 흐름 활성화 |
| **3순위 — 온보딩** | 2.3, 2.4, 6.1~6.4 (응원팀 등록 + cascading) | 온보딩/마이페이지 |
| **4순위 — 게시글 메타** | 3.5 (투표 API), 3.1~3.4 (페이로드/응답 확장) | 작성·열람 |
| **5순위 — 팀 위젯** | 4.1, 5.1 (대시보드/통계) | 팀 게시판/메인 |
| **6순위 — 권한 분리** | 7 (inquiries/requests 본인 한정) | 운영 안정성 |

---

## 9. 프론트엔드 측 mock 핸들러 위치 (참고)

백엔드 구현 후 프론트의 다음 mock 핸들러를 제거하면 자동으로 실서버를 바라봅니다:

```
locker-room-web-service/src/mocks/handlers.ts

# 신규 mock (백엔드 구현 후 제거)
- POST /auth/phone/verification
- POST /auth/phone/verification/confirm
- POST /auth/email/verification/resend
- POST /auth/email/verification/confirm
- POST /users/me/teams
- POST /users/me/onboarding/skip
- POST /posts/:postId/vote
- GET  /teams/:teamId/dashboard
- GET  /stats/teams/ranking

# 기존 mock 응답 갱신 필요 (필드 추가)
- GET  /users/me           (emailVerified, phone, onboardingCompletedAt 반영)
- PUT  /users/me           (profileImageUrl 처리)
- POST /auth/signup        (phone 받기)
- POST /auth/profile/complete (teams 제거)
- POST /posts              (category, poll 받기)
- PUT  /posts/:postId      (category 받기)
- GET  /posts/:postId      (category, poll 응답)
- GET  /boards/:boardId/posts (category, hasPoll 응답)
- GET  /posts/popular      (category, hasPoll 응답)
```

각 mock의 형식이 위 명세와 일치하므로 **contract 그대로 백엔드에 구현하면 프론트는 추가 변경 없이 바로 연결**됩니다.
