# Locker Room - API 상세 명세서

> 참고 문서: `back-end/decide.md`
> Base URL: `{gateway}/api/v1`

---

## 공통 사항

### 인증 헤더
```
Authorization: Bearer {accessToken}
```

### 멱등성 키 (POST 요청)
```
Idempotency-Key: {UUID}
```

### 공통 응답 형식

**성공**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { ... }
}
```

**실패**
```json
{
  "code": "POST_NOT_FOUND",
  "message": "게시글을 찾을 수 없습니다.",
  "data": null
}
```

### 페이지네이션 응답 형식 (Cursor 기반)
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [ ... ],
    "nextCursor": "eyJpZCI6MTAwfQ==",
    "hasNext": true
  }
}
```

### 공통 쿼리 파라미터 (목록 조회)
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| cursor | string | X | null | 이전 응답의 nextCursor 값 |
| size | int | X | 20 | 조회 개수 (최대 100) |
| sort | string | X | created_at | 정렬 기준 (created_at, like_count) |

### HTTP 상태 코드
| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 204 | 삭제 성공 (응답 본문 없음) |
| 400 | 잘못된 요청 (유효성 검증 실패) |
| 401 | 인증 필요 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 중복 요청 (멱등키) |
| 429 | Rate Limit 초과 |
| 500 | 서버 내부 오류 |

### 유효성 검증 실패 응답
```json
{
  "code": "COMMON_INVALID_PARAMETER",
  "message": "입력값이 올바르지 않습니다.",
  "data": {
    "errors": [
      { "field": "email", "message": "올바른 이메일 형식이 아닙니다." },
      { "field": "password", "message": "비밀번호는 8자 이상이어야 합니다." }
    ]
  }
}
```

---

## 1. auth-service (`/api/v1/auth`)

> **TBD**: 인증 서버 구현 방식 (자체 JWT vs Keycloak/OIDC) 미결정. 아래 명세는 자체 JWT 구현 기준이며, Keycloak 도입 시 일부 API 스펙이 변경될 수 있음.

### 1.1 POST `/auth/signup` - 로컬 회원가입

**인증**: 불필요

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "Password1!",
  "nickname": "축구팬",
  "teams": [
    { "sportId": 1, "teamId": 3 }
  ]
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| email | string | O | 이메일 형식, 최대 255자 |
| password | string | O | 8~20자, 영문+숫자+특수문자 포함 |
| nickname | string | O | 2~20자, 특수문자 불가 |
| teams | array | O | 최소 1개 종목, 종목당 1팀 |
| teams[].sportId | long | O | 존재하는 종목 ID |
| teams[].teamId | long | O | 해당 종목의 팀 ID |

**Response 201**
```json
{
  "code": "SUCCESS",
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "userId": 42,
    "email": "user@example.com",
    "nickname": "축구팬"
  }
}
```

**에러 코드**
| 코드 | 상태 | 설명 |
|------|------|------|
| USER_EMAIL_DUPLICATED | 409 | 이미 사용 중인 이메일 |
| USER_NICKNAME_DUPLICATED | 409 | 이미 사용 중인 닉네임 |
| COMMON_INVALID_PARAMETER | 400 | 유효성 검증 실패 |

---

### 1.2 POST `/auth/login` - 로컬 로그인

**인증**: 불필요

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "Password1!"
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| email | string | O | 이메일 형식 |
| password | string | O | 비밀번호 |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "로그인되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJl...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 42,
      "email": "user@example.com",
      "nickname": "축구팬",
      "role": "USER"
    }
  }
}
```

**에러 코드**
| 코드 | 상태 | 설명 |
|------|------|------|
| AUTH_INVALID_CREDENTIALS | 401 | 이메일 또는 비밀번호 불일치 |
| USER_SUSPENDED | 403 | 정지된 계정 |
| COMMON_RATE_LIMIT_EXCEEDED | 429 | 로그인 시도 횟수 초과 |

---

### 1.3 POST `/auth/logout` - 로그아웃

**인증**: 필수

**Request Header**
```
Authorization: Bearer {accessToken}
```

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "로그아웃되었습니다.",
  "data": null
}
```

> Access Token은 Redis 블랙리스트에 등록되며, 남은 만료 시간만큼 유지된다.

---

### 1.4 POST `/auth/token/refresh` - 토큰 갱신

**인증**: 불필요

**Request Body**
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJl..."
}
```

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "토큰이 갱신되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "bmV3IHJlZnJlc2ggdG9r...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

**에러 코드**
| 코드 | 상태 | 설명 |
|------|------|------|
| AUTH_TOKEN_EXPIRED | 401 | Refresh Token 만료 |
| AUTH_TOKEN_INVALID | 401 | 유효하지 않은 Refresh Token |

---

### 1.5 GET `/auth/oauth/{provider}` - SSO 로그인 시작

**인증**: 불필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| provider | string | SSO 제공자 (`google`, `kakao`, `naver`) |

**Response**: OAuth Provider 로그인 페이지로 302 Redirect

---

### 1.6 GET `/auth/oauth/{provider}/callback` - SSO 콜백

**인증**: 불필요

**Query Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| code | string | OAuth Authorization Code |
| state | string | CSRF 방지 state 값 |

**Response 200** (기존 회원)
```json
{
  "code": "SUCCESS",
  "message": "로그인되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJl...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "isNewUser": false,
    "user": {
      "id": 42,
      "email": "user@example.com",
      "nickname": "축구팬",
      "role": "USER"
    }
  }
}
```

**Response 200** (신규 회원 - 팀 선택 필요)
```json
{
  "code": "SUCCESS",
  "message": "추가 정보 입력이 필요합니다.",
  "data": {
    "isNewUser": true,
    "tempToken": "dGVtcG9yYXJ5IHRva2Vu...",
    "email": "user@gmail.com",
    "provider": "GOOGLE"
  }
}
```

> 신규 SSO 회원은 `tempToken`을 사용하여 닉네임/팀 선택 후 가입을 완료해야 한다.

---

### 1.7 POST `/auth/oauth/complete` - SSO 가입 완료

**인증**: 불필요 (tempToken으로 인증)

> SSO 콜백에서 `isNewUser: true`를 받은 신규 회원이 닉네임과 응원팀을 선택하여 가입을 완료한다.

**Request Body**
```json
{
  "tempToken": "dGVtcG9yYXJ5IHRva2Vu...",
  "nickname": "축구팬",
  "teams": [
    { "sportId": 1, "teamId": 3 }
  ]
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| tempToken | string | O | SSO 콜백에서 발급된 임시 토큰 |
| nickname | string | O | 2~20자, 특수문자 불가 |
| teams | array | O | 최소 1개 종목, 종목당 1팀 |
| teams[].sportId | long | O | 존재하는 종목 ID |
| teams[].teamId | long | O | 해당 종목의 팀 ID |

**Response 201**
```json
{
  "code": "SUCCESS",
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJl...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 43,
      "email": "user@gmail.com",
      "nickname": "축구팬",
      "role": "USER",
      "provider": "GOOGLE"
    }
  }
}
```

> 가입 완료 즉시 Access Token + Refresh Token을 발급하여 별도 로그인 없이 서비스 이용 가능.

**에러 코드**
| 코드 | 상태 | 설명 |
|------|------|------|
| AUTH_TOKEN_EXPIRED | 401 | tempToken 만료 (10분) |
| AUTH_TOKEN_INVALID | 401 | 유효하지 않은 tempToken |
| USER_NICKNAME_DUPLICATED | 409 | 이미 사용 중인 닉네임 |
| COMMON_INVALID_PARAMETER | 400 | 유효성 검증 실패 |

---

### 1.8 POST `/auth/password/find` - 비밀번호 재설정 메일 발송

**인증**: 불필요

**Request Body**
```json
{
  "email": "user@example.com"
}
```

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "비밀번호 재설정 메일이 발송되었습니다.",
  "data": null
}
```

> 존재하지 않는 이메일이라도 동일한 응답을 반환한다 (보안).

---

### 1.9 PUT `/auth/password/reset` - 비밀번호 재설정

**인증**: 불필요

**Request Body**
```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewPassword1!"
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| token | string | O | 이메일로 발송된 재설정 토큰 |
| newPassword | string | O | 8~20자, 영문+숫자+특수문자 포함 |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "비밀번호가 변경되었습니다.",
  "data": null
}
```

**에러 코드**
| 코드 | 상태 | 설명 |
|------|------|------|
| AUTH_TOKEN_EXPIRED | 401 | 재설정 토큰 만료 |
| AUTH_TOKEN_INVALID | 401 | 유효하지 않은 재설정 토큰 |

---

## 2. resource-service (`/api/v1`)

### 2.1 사용자 (Users)

#### 2.1.1 GET `/users/me` - 내 정보 조회

**인증**: 필수

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 42,
    "email": "user@example.com",
    "nickname": "축구팬",
    "role": "USER",
    "provider": null,
    "teams": [
      {
        "sportId": 1,
        "sportName": "축구",
        "teamId": 3,
        "teamName": "전북 현대 모터스",
        "teamLogoUrl": "https://s3.../logos/jeonbuk.png"
      }
    ],
    "createdAt": "2026-01-15T09:00:00Z"
  }
}
```

---

#### 2.1.2 PUT `/users/me` - 내 정보 수정

**인증**: 필수

**Request Body**
```json
{
  "nickname": "새닉네임",
  "currentPassword": "Password1!",
  "newPassword": "NewPassword1!"
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| nickname | string | X | 2~20자, 특수문자 불가 |
| currentPassword | string | X | 비밀번호 변경 시 필수 |
| newPassword | string | X | 8~20자, 영문+숫자+특수문자 |

> 닉네임만 변경할 경우 비밀번호 필드 생략 가능. 비밀번호 변경 시 currentPassword 필수.

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "정보가 수정되었습니다.",
  "data": {
    "id": 42,
    "nickname": "새닉네임"
  }
}
```

---

#### 2.1.3 DELETE `/users/me` - 회원 탈퇴

**인증**: 필수

**Request Body**
```json
{
  "reason": "더 이상 사용하지 않습니다.",
  "password": "Password1!"
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| reason | string | X | 탈퇴 사유 |
| password | string | O | 본인 확인용 비밀번호 (로컬 계정) |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "회원 탈퇴가 완료되었습니다.",
  "data": null
}
```

> Soft Delete 처리. user_withdrawals 테이블에 이력 기록.

---

#### 2.1.4 GET `/users/me/posts` - 내가 쓴 글 목록

**인증**: 필수

**Query Parameter**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| cursor | string | X | null | 커서 |
| size | int | X | 20 | 조회 개수 |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [
      {
        "id": 101,
        "boardId": 1,
        "boardName": "공통 게시판",
        "title": "오늘 경기 어땠나요?",
        "viewCount": 150,
        "likeCount": 12,
        "commentCount": 8,
        "createdAt": "2026-02-14T15:30:00Z"
      }
    ],
    "nextCursor": "eyJpZCI6MTAwfQ==",
    "hasNext": true
  }
}
```

---

#### 2.1.5 GET `/users/me/comments` - 내가 쓴 댓글 목록

**인증**: 필수

**Query Parameter**: cursor, size (공통)

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [
      {
        "id": 501,
        "postId": 101,
        "postTitle": "오늘 경기 어땠나요?",
        "content": "정말 재미있었어요!",
        "createdAt": "2026-02-14T16:00:00Z"
      }
    ],
    "nextCursor": "eyJpZCI6NTAwfQ==",
    "hasNext": false
  }
}
```

---

#### 2.1.6 GET `/users/me/likes` - 좋아요한 글 목록

**인증**: 필수

**Query Parameter**: cursor, size (공통)

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [
      {
        "id": 102,
        "boardId": 4,
        "boardName": "전북 현대 모터스 게시판",
        "title": "이번 시즌 전망",
        "authorNickname": "전북팬",
        "viewCount": 320,
        "likeCount": 45,
        "commentCount": 22,
        "createdAt": "2026-02-13T10:00:00Z"
      }
    ],
    "nextCursor": "eyJpZCI6MTAxfQ==",
    "hasNext": true
  }
}
```

---

### 2.2 종목/팀 (Sports & Teams)

#### 2.2.1 GET `/sports` - 종목 목록 조회

**인증**: 불필요

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    { "id": 1, "name": "축구", "isActive": true },
    { "id": 2, "name": "야구", "isActive": true }
  ]
}
```

---

#### 2.2.2 GET `/sports/{sportId}/teams` - 팀 목록 조회

**인증**: 불필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| sportId | long | 종목 ID |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    {
      "id": 1,
      "name": "울산 HD FC",
      "logoUrl": "https://s3.../logos/ulsan.png",
      "isActive": true
    },
    {
      "id": 2,
      "name": "김천 상무 FC",
      "logoUrl": "https://s3.../logos/gimcheon.png",
      "isActive": true
    }
  ]
}
```

---

### 2.3 게시판 (Boards)

#### 2.3.1 GET `/boards` - 게시판 목록 조회

**인증**: 선택 (인증 시 팀 전용 게시판 포함)

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    {
      "id": 1,
      "name": "공통 게시판",
      "type": "COMMON",
      "teamId": null,
      "teamName": null
    },
    {
      "id": 2,
      "name": "Q&A 게시판",
      "type": "QNA",
      "teamId": null,
      "teamName": null
    },
    {
      "id": 4,
      "name": "전북 현대 모터스 게시판",
      "type": "TEAM",
      "teamId": 3,
      "teamName": "전북 현대 모터스"
    }
  ]
}
```

---

#### 2.3.2 GET `/boards/{boardId}/posts` - 게시판별 게시글 목록

**인증**: 선택 (팀 전용 게시판은 해당 팀 회원만)

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| boardId | long | 게시판 ID |

**Query Parameter**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| cursor | string | X | null | 커서 |
| size | int | X | 20 | 조회 개수 |
| sort | string | X | created_at | 정렬 (created_at, like_count) |
| keyword | string | X | null | 검색 키워드 |
| searchType | string | X | TITLE_CONTENT | TITLE, CONTENT, TITLE_CONTENT, NICKNAME |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [
      {
        "id": 101,
        "title": "오늘 경기 어땠나요?",
        "authorNickname": "축구팬",
        "viewCount": 150,
        "likeCount": 12,
        "commentCount": 8,
        "isAiGenerated": false,
        "createdAt": "2026-02-14T15:30:00Z"
      }
    ],
    "nextCursor": "eyJpZCI6MTAwfQ==",
    "hasNext": true
  }
}
```

**에러 코드**
| 코드 | 상태 | 설명 |
|------|------|------|
| POST_ACCESS_DENIED | 403 | 팀 전용 게시판 접근 권한 없음 |

---

### 2.4 게시글 (Posts)

#### 2.4.1 POST `/posts` - 게시글 작성

**인증**: 필수
**멱등키**: 필수

**Request Body**
```json
{
  "boardId": 1,
  "title": "오늘 경기 어땠나요?",
  "content": "오늘 경기 정말 재미있었는데 다들 어떻게 생각하세요?",
  "fileIds": [1, 2]
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| boardId | long | O | 존재하는 게시판 ID |
| title | string | O | 1~200자 |
| content | string | O | 1~10000자 |
| fileIds | array[long] | X | 최대 5개, 사전 업로드된 파일 ID |

**Response 201**
```json
{
  "code": "SUCCESS",
  "message": "게시글이 작성되었습니다.",
  "data": {
    "id": 101,
    "boardId": 1,
    "title": "오늘 경기 어땠나요?",
    "createdAt": "2026-02-14T15:30:00Z"
  }
}
```

> Q&A 게시판(type=QNA) 게시글 작성 시 `qna-post.created` Kafka 이벤트 발행 → AI 자동 답변 트리거

**에러 코드**
| 코드 | 상태 | 설명 |
|------|------|------|
| POST_ACCESS_DENIED | 403 | 해당 게시판 작성 권한 없음 |
| COMMON_DUPLICATE_REQUEST | 409 | 멱등키 중복 |
| FILE_SIZE_EXCEEDED | 400 | 첨부파일 개수 초과 |

---

#### 2.4.2 GET `/posts/{postId}` - 게시글 상세 조회

**인증**: 선택 (팀 전용 게시판은 인증 필수)

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| postId | long | 게시글 ID |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 101,
    "boardId": 1,
    "boardName": "공통 게시판",
    "author": {
      "id": 42,
      "nickname": "축구팬",
      "teamName": "전북 현대 모터스"
    },
    "title": "오늘 경기 어땠나요?",
    "content": "오늘 경기 정말 재미있었는데 다들 어떻게 생각하세요?",
    "viewCount": 151,
    "likeCount": 12,
    "commentCount": 8,
    "isAiGenerated": false,
    "isLiked": true,
    "files": [
      {
        "id": 1,
        "originalName": "screenshot.png",
        "url": "https://s3.../files/abc123.png",
        "size": 1048576,
        "mimeType": "image/png"
      }
    ],
    "createdAt": "2026-02-14T15:30:00Z",
    "updatedAt": "2026-02-14T15:30:00Z"
  }
}
```

> `isLiked`는 인증된 사용자에게만 제공 (비인증 시 false)

**에러 코드**
| 코드 | 상태 | 설명 |
|------|------|------|
| POST_NOT_FOUND | 404 | 게시글 없음 |
| POST_ACCESS_DENIED | 403 | 접근 권한 없음 |

---

#### 2.4.3 PUT `/posts/{postId}` - 게시글 수정

**인증**: 필수 (작성자 본인)

**Request Body**
```json
{
  "title": "수정된 제목",
  "content": "수정된 내용입니다.",
  "fileIds": [1, 3]
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| title | string | O | 1~200자 |
| content | string | O | 1~10000자 |
| fileIds | array[long] | X | 최대 5개 |

> 기존 fileIds에 없는 파일은 자동 삭제(S3 포함)

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "게시글이 수정되었습니다.",
  "data": {
    "id": 101,
    "title": "수정된 제목",
    "updatedAt": "2026-02-14T16:00:00Z"
  }
}
```

**에러 코드**
| 코드 | 상태 | 설명 |
|------|------|------|
| POST_NOT_FOUND | 404 | 게시글 없음 |
| POST_ACCESS_DENIED | 403 | 작성자가 아님 |

---

#### 2.4.4 DELETE `/posts/{postId}` - 게시글 삭제

**인증**: 필수 (작성자 본인 또는 관리자)

**Response 204**: 응답 본문 없음

**에러 코드**
| 코드 | 상태 | 설명 |
|------|------|------|
| POST_NOT_FOUND | 404 | 게시글 없음 |
| POST_ACCESS_DENIED | 403 | 삭제 권한 없음 |

> Soft Delete 처리. 첨부파일은 S3에서도 삭제.

---

#### 2.4.5 POST `/posts/{postId}/like` - 좋아요 토글

**인증**: 필수
**멱등키**: 필수

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "좋아요가 반영되었습니다.",
  "data": {
    "postId": 101,
    "isLiked": true,
    "likeCount": 13
  }
}
```

> 이미 좋아요한 상태에서 재요청 시 좋아요 해제 (토글 방식)

---

#### 2.4.6 POST `/posts/{postId}/report` - 게시글 신고

**인증**: 필수
**멱등키**: 필수

**Request Body**
```json
{
  "reason": "부적절한 내용이 포함되어 있습니다."
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| reason | string | O | 1~500자 |

**Response 201**
```json
{
  "code": "SUCCESS",
  "message": "신고가 접수되었습니다.",
  "data": {
    "reportId": 10,
    "postId": 101,
    "status": "PENDING"
  }
}
```

**에러 코드**
| 코드 | 상태 | 설명 |
|------|------|------|
| POST_NOT_FOUND | 404 | 게시글 없음 |
| POST_ALREADY_REPORTED | 409 | 이미 신고한 게시글 |

---

### 2.5 댓글 (Comments)

#### 2.5.1 GET `/posts/{postId}/comments` - 댓글 목록 조회

**인증**: 선택

**Query Parameter**: cursor, size (공통)

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [
      {
        "id": 501,
        "author": {
          "id": 43,
          "nickname": "야구팬",
          "teamName": "삼성 라이온즈"
        },
        "content": "정말 좋은 글이네요!",
        "isAiGenerated": false,
        "createdAt": "2026-02-14T16:00:00Z",
        "replies": [
          {
            "id": 502,
            "author": {
              "id": 42,
              "nickname": "축구팬",
              "teamName": "전북 현대 모터스"
            },
            "content": "감사합니다!",
            "isAiGenerated": false,
            "createdAt": "2026-02-14T16:05:00Z"
          }
        ]
      }
    ],
    "nextCursor": "eyJpZCI6NTAwfQ==",
    "hasNext": false
  }
}
```

> 대댓글(replies)은 부모 댓글에 중첩하여 반환. depth 1단계 제한.

---

#### 2.5.2 POST `/posts/{postId}/comments` - 댓글 작성

**인증**: 필수
**멱등키**: 필수

**Request Body**
```json
{
  "content": "정말 좋은 글이네요!"
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| content | string | O | 1~1000자 |

**Response 201**
```json
{
  "code": "SUCCESS",
  "message": "댓글이 작성되었습니다.",
  "data": {
    "id": 503,
    "postId": 101,
    "content": "정말 좋은 글이네요!",
    "createdAt": "2026-02-14T17:00:00Z"
  }
}
```

> 게시글 작성자에게 `notification.comment` Kafka 이벤트 발행

---

#### 2.5.3 PUT `/comments/{commentId}` - 댓글 수정

**인증**: 필수 (작성자 본인)

**Request Body**
```json
{
  "content": "수정된 댓글입니다."
}
```

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "댓글이 수정되었습니다.",
  "data": {
    "id": 503,
    "content": "수정된 댓글입니다.",
    "updatedAt": "2026-02-14T17:30:00Z"
  }
}
```

---

#### 2.5.4 DELETE `/comments/{commentId}` - 댓글 삭제

**인증**: 필수 (작성자 본인 또는 관리자)

**Response 204**: 응답 본문 없음

> Soft Delete 처리. 대댓글이 있는 댓글 삭제 시 "삭제된 댓글입니다."로 표시.

---

#### 2.5.5 POST `/comments/{commentId}/replies` - 대댓글 작성

**인증**: 필수
**멱등키**: 필수

**Request Body**
```json
{
  "content": "대댓글 내용입니다."
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| content | string | O | 1~1000자 |

**Response 201**
```json
{
  "code": "SUCCESS",
  "message": "대댓글이 작성되었습니다.",
  "data": {
    "id": 504,
    "parentId": 501,
    "postId": 101,
    "content": "대댓글 내용입니다.",
    "createdAt": "2026-02-14T17:10:00Z"
  }
}
```

> 부모 댓글 작성자에게 `notification.reply` Kafka 이벤트 발행

**에러 코드**
| 코드 | 상태 | 설명 |
|------|------|------|
| COMMENT_NOT_FOUND | 404 | 부모 댓글 없음 |
| COMMENT_REPLY_DEPTH_EXCEEDED | 400 | 대댓글에 대한 대댓글 불가 (depth 1 제한) |

---

### 2.6 공지사항 (Notices)

#### 2.6.1 GET `/notices` - 공지사항 목록

**인증**: 불필요

**Query Parameter**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| cursor | string | X | null | 커서 |
| size | int | X | 20 | 조회 개수 |
| teamId | long | X | null | 팀 공지 필터 (null이면 전체 공지만) |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [
      {
        "id": 1,
        "title": "서비스 오픈 안내",
        "isPinned": true,
        "scope": "ALL",
        "teamName": null,
        "createdAt": "2026-02-01T00:00:00Z"
      }
    ],
    "nextCursor": null,
    "hasNext": false
  }
}
```

> 상단 고정(isPinned=true) 공지가 먼저 노출

---

#### 2.6.2 GET `/notices/{noticeId}` - 공지사항 상세

**인증**: 불필요

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "title": "서비스 오픈 안내",
    "content": "라커룸 서비스가 정식 오픈되었습니다...",
    "isPinned": true,
    "scope": "ALL",
    "teamId": null,
    "teamName": null,
    "adminNickname": "관리자",
    "createdAt": "2026-02-01T00:00:00Z",
    "updatedAt": "2026-02-01T00:00:00Z"
  }
}
```

---

### 2.7 고객센터 (Support)

#### 2.7.1 POST `/inquiries` - 1:1 문의 작성

**인증**: 필수
**멱등키**: 필수

**Request Body**
```json
{
  "type": "BUG",
  "title": "로그인이 안 됩니다",
  "content": "구글 로그인 시 오류가 발생합니다. 에러 화면을 첨부합니다.",
  "fileIds": [5]
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| type | string | O | GENERAL, BUG, SUGGESTION |
| title | string | O | 1~200자 |
| content | string | O | 1~5000자 |
| fileIds | array[long] | X | 최대 5개 |

**Response 201**
```json
{
  "code": "SUCCESS",
  "message": "문의가 접수되었습니다.",
  "data": {
    "id": 10,
    "type": "BUG",
    "title": "로그인이 안 됩니다",
    "status": "PENDING",
    "createdAt": "2026-02-14T18:00:00Z"
  }
}
```

---

#### 2.7.2 GET `/inquiries` - 내 문의 목록

**인증**: 필수

**Query Parameter**: cursor, size (공통)

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [
      {
        "id": 10,
        "type": "BUG",
        "title": "로그인이 안 됩니다",
        "status": "ANSWERED",
        "createdAt": "2026-02-14T18:00:00Z"
      }
    ],
    "nextCursor": null,
    "hasNext": false
  }
}
```

---

#### 2.7.3 GET `/inquiries/{inquiryId}` - 문의 상세 조회

**인증**: 필수 (본인 문의만)

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 10,
    "type": "BUG",
    "title": "로그인이 안 됩니다",
    "content": "구글 로그인 시 오류가 발생합니다.",
    "status": "ANSWERED",
    "files": [
      {
        "id": 5,
        "originalName": "error_screenshot.png",
        "url": "https://s3.../files/def456.png",
        "size": 524288,
        "mimeType": "image/png"
      }
    ],
    "replies": [
      {
        "id": 1,
        "adminNickname": "관리자",
        "content": "해당 문제를 확인하여 수정 완료하였습니다.",
        "createdAt": "2026-02-15T09:00:00Z"
      }
    ],
    "createdAt": "2026-02-14T18:00:00Z"
  }
}
```

---

### 2.8 요청 (Requests)

#### 2.8.1 POST `/requests` - 종목/구단 추가 요청

**인증**: 필수
**멱등키**: 필수

**Request Body**
```json
{
  "type": "TEAM",
  "name": "수원 삼성 블루윙즈",
  "reason": "K리그2 팀도 추가해주세요."
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| type | string | O | SPORT, TEAM |
| name | string | O | 1~100자 |
| reason | string | O | 1~1000자 |

**Response 201**
```json
{
  "code": "SUCCESS",
  "message": "요청이 접수되었습니다.",
  "data": {
    "id": 5,
    "type": "TEAM",
    "name": "수원 삼성 블루윙즈",
    "status": "PENDING",
    "createdAt": "2026-02-14T19:00:00Z"
  }
}
```

---

#### 2.8.2 GET `/requests` - 내 요청 목록

**인증**: 필수

**Query Parameter**: cursor, size (공통)

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [
      {
        "id": 5,
        "type": "TEAM",
        "name": "수원 삼성 블루윙즈",
        "status": "PENDING",
        "createdAt": "2026-02-14T19:00:00Z"
      }
    ],
    "nextCursor": null,
    "hasNext": false
  }
}
```

---

#### 2.8.3 GET `/requests/{requestId}` - 요청 상세 조회

**인증**: 필수 (본인 요청만)

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 5,
    "type": "TEAM",
    "name": "수원 삼성 블루윙즈",
    "reason": "K리그2 팀도 추가해주세요.",
    "status": "REJECTED",
    "rejectReason": "현재 K리그1 팀만 지원하고 있습니다.",
    "processedAt": "2026-02-15T10:00:00Z",
    "createdAt": "2026-02-14T19:00:00Z"
  }
}
```

---

### 2.9 알림 (Notifications)

#### 2.9.1 GET `/notifications` - 내 알림 목록

**인증**: 필수

**Query Parameter**: cursor, size (공통)

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [
      {
        "id": 1001,
        "type": "COMMENT",
        "targetType": "POST",
        "targetId": 101,
        "message": "축구팬님이 회원님의 게시글에 댓글을 남겼습니다.",
        "isRead": false,
        "createdAt": "2026-02-14T16:00:00Z"
      },
      {
        "id": 1002,
        "type": "REPLY",
        "targetType": "COMMENT",
        "targetId": 501,
        "message": "야구팬님이 회원님의 댓글에 답글을 남겼습니다.",
        "isRead": true,
        "readAt": "2026-02-14T17:00:00Z",
        "createdAt": "2026-02-14T16:30:00Z"
      }
    ],
    "nextCursor": "eyJpZCI6MTAwMH0=",
    "hasNext": true
  }
}
```

---

#### 2.9.2 GET `/notifications/unread-count` - 읽지 않은 알림 수

**인증**: 필수

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "unreadCount": 3
  }
}
```

---

#### 2.9.3 PUT `/notifications/{notificationId}/read` - 알림 읽음 처리

**인증**: 필수

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "알림을 읽음 처리하였습니다.",
  "data": null
}
```

---

#### 2.9.4 PUT `/notifications/read-all` - 전체 알림 읽음 처리

**인증**: 필수

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "모든 알림을 읽음 처리하였습니다.",
  "data": {
    "updatedCount": 3
  }
}
```

---

### 2.10 파일 (Files)

#### 2.10.1 POST `/files` - 파일 업로드

**인증**: 필수

**Request**: `multipart/form-data`
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| file | file | O | 업로드할 파일 |
| targetType | string | O | POST, INQUIRY, COMMENT |

**파일 제한**
| 항목 | 제한 |
|------|------|
| 이미지 파일 | 최대 10MB |
| 일반 파일 | 최대 20MB |
| 허용 이미지 타입 | image/jpeg, image/png, image/gif, image/webp |
| 허용 일반 타입 | application/pdf, text/plain |

**Response 201**
```json
{
  "code": "SUCCESS",
  "message": "파일이 업로드되었습니다.",
  "data": {
    "id": 1,
    "originalName": "screenshot.png",
    "url": "https://s3.../files/abc123.png",
    "size": 1048576,
    "mimeType": "image/png"
  }
}
```

**에러 코드**
| 코드 | 상태 | 설명 |
|------|------|------|
| FILE_SIZE_EXCEEDED | 400 | 파일 크기 초과 |
| FILE_TYPE_NOT_ALLOWED | 400 | 허용되지 않은 파일 타입 |

---

#### 2.10.2 DELETE `/files/{fileId}` - 파일 삭제

**인증**: 필수 (업로더 본인)

**Response 204**: 응답 본문 없음

> S3에서도 물리적으로 삭제

---

### 2.11 관리자 (Admin)

> 모든 관리자 API는 `role=ADMIN` 권한이 필요합니다.

#### 2.11.1 GET `/admin/users` - 회원 목록 조회

**인증**: 필수 (ADMIN)

**Query Parameter**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| cursor | string | X | null | 커서 |
| size | int | X | 20 | 조회 개수 |
| keyword | string | X | null | 닉네임/이메일 검색 |
| role | string | X | null | 역할 필터 (USER, ADMIN) |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [
      {
        "id": 42,
        "email": "user@example.com",
        "nickname": "축구팬",
        "role": "USER",
        "provider": null,
        "isSuspended": false,
        "createdAt": "2026-01-15T09:00:00Z"
      }
    ],
    "nextCursor": "eyJpZCI6NDF9",
    "hasNext": true
  }
}
```

---

#### 2.11.2 PUT `/admin/users/{userId}/suspend` - 회원 정지 처리

**인증**: 필수 (ADMIN)

**Request Body**
```json
{
  "reason": "커뮤니티 규정 위반 (욕설, 비방)",
  "suspendedUntil": "2026-03-14T23:59:59Z"
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| reason | string | O | 1~1000자 |
| suspendedUntil | datetime | O | 현재 시각 이후 |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "회원 정지가 처리되었습니다.",
  "data": {
    "userId": 42,
    "suspendedAt": "2026-02-14T20:00:00Z",
    "suspendedUntil": "2026-03-14T23:59:59Z"
  }
}
```

---

#### 2.11.3 GET `/admin/reports` - 신고 목록 조회

**인증**: 필수 (ADMIN)

**Query Parameter**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| cursor | string | X | null | 커서 |
| size | int | X | 20 | 조회 개수 |
| status | string | X | null | PENDING, APPROVED, REJECTED |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [
      {
        "id": 10,
        "postId": 101,
        "postTitle": "문제 게시글",
        "reporterNickname": "신고자닉네임",
        "reason": "부적절한 내용",
        "status": "PENDING",
        "createdAt": "2026-02-14T18:00:00Z"
      }
    ],
    "nextCursor": null,
    "hasNext": false
  }
}
```

---

#### 2.11.4 PUT `/admin/reports/{reportId}` - 신고 처리

**인증**: 필수 (ADMIN)

**Request Body**
```json
{
  "status": "APPROVED",
  "action": "DELETE_POST"
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| status | string | O | APPROVED, REJECTED |
| action | string | X | DELETE_POST, SUSPEND_USER (APPROVED 시) |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "신고가 처리되었습니다.",
  "data": {
    "reportId": 10,
    "status": "APPROVED",
    "processedAt": "2026-02-15T09:00:00Z"
  }
}
```

> 처리 결과는 `notification.report-processed` Kafka 이벤트로 신고자에게 알림

---

#### 2.11.5 POST `/admin/notices` - 공지사항 작성

**인증**: 필수 (ADMIN)

**Request Body**
```json
{
  "title": "서비스 점검 안내",
  "content": "2026년 2월 20일 02:00~06:00 서비스 점검이 예정되어 있습니다.",
  "isPinned": true,
  "scope": "ALL",
  "teamId": null
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| title | string | O | 1~200자 |
| content | string | O | 1~10000자 |
| isPinned | boolean | X | 기본 false |
| scope | string | O | ALL, TEAM |
| teamId | long | X | scope=TEAM일 때 필수 |

**Response 201**
```json
{
  "code": "SUCCESS",
  "message": "공지사항이 작성되었습니다.",
  "data": {
    "id": 2,
    "title": "서비스 점검 안내",
    "createdAt": "2026-02-15T10:00:00Z"
  }
}
```

---

#### 2.11.6 PUT `/admin/notices/{noticeId}` - 공지사항 수정

**인증**: 필수 (ADMIN)

**Request Body**: 공지사항 작성과 동일

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "공지사항이 수정되었습니다.",
  "data": {
    "id": 2,
    "title": "서비스 점검 안내 (시간 변경)",
    "updatedAt": "2026-02-15T11:00:00Z"
  }
}
```

---

#### 2.11.7 DELETE `/admin/notices/{noticeId}` - 공지사항 삭제

**인증**: 필수 (ADMIN)

**Response 204**: 응답 본문 없음

---

#### 2.11.8 GET `/admin/inquiries` - 문의 목록 조회

**인증**: 필수 (ADMIN)

**Query Parameter**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| cursor | string | X | null | 커서 |
| size | int | X | 20 | 조회 개수 |
| status | string | X | null | PENDING, ANSWERED |
| type | string | X | null | GENERAL, BUG, SUGGESTION |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [
      {
        "id": 10,
        "userNickname": "축구팬",
        "type": "BUG",
        "title": "로그인이 안 됩니다",
        "status": "PENDING",
        "createdAt": "2026-02-14T18:00:00Z"
      }
    ],
    "nextCursor": null,
    "hasNext": false
  }
}
```

---

#### 2.11.9 POST `/admin/inquiries/{inquiryId}/reply` - 문의 답변 작성

**인증**: 필수 (ADMIN)

**Request Body**
```json
{
  "content": "해당 문제를 확인하여 수정 완료하였습니다. 다시 시도해주세요."
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| content | string | O | 1~5000자 |

**Response 201**
```json
{
  "code": "SUCCESS",
  "message": "답변이 작성되었습니다.",
  "data": {
    "id": 1,
    "inquiryId": 10,
    "createdAt": "2026-02-15T09:00:00Z"
  }
}
```

> 문의 status가 ANSWERED로 변경. `notification.inquiry-replied` Kafka 이벤트 발행.

---

#### 2.11.10 GET `/admin/requests` - 요청 목록 조회

**인증**: 필수 (ADMIN)

**Query Parameter**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| cursor | string | X | null | 커서 |
| size | int | X | 20 | 조회 개수 |
| status | string | X | null | PENDING, APPROVED, REJECTED |
| type | string | X | null | SPORT, TEAM |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "items": [
      {
        "id": 5,
        "userNickname": "축구팬",
        "type": "TEAM",
        "name": "수원 삼성 블루윙즈",
        "reason": "K리그2 팀도 추가해주세요.",
        "status": "PENDING",
        "createdAt": "2026-02-14T19:00:00Z"
      }
    ],
    "nextCursor": null,
    "hasNext": false
  }
}
```

---

#### 2.11.11 PUT `/admin/requests/{requestId}` - 요청 처리

**인증**: 필수 (ADMIN)

**Request Body**
```json
{
  "status": "REJECTED",
  "rejectReason": "현재 K리그1 팀만 지원하고 있습니다."
}
```

| 필드 | 타입 | 필수 | 유효성 검증 |
|------|------|------|-------------|
| status | string | O | APPROVED, REJECTED |
| rejectReason | string | X | REJECTED 시 필수, 1~1000자 |

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "요청이 처리되었습니다.",
  "data": {
    "requestId": 5,
    "status": "REJECTED",
    "processedAt": "2026-02-15T10:00:00Z"
  }
}
```

> APPROVED 시 해당 종목/팀이 자동 생성되고, 관련 게시판도 함께 생성.

---

## 3. ai-service (`/api/v1/ai`)

> ai-service의 API는 내부 서비스 간 호출 전용 (Client Credentials 인증)
> **TBD**: LLM 모델 미결정. 모델 선정에 따라 응답 형식, 처리 시간, 비용 등이 달라질 수 있음.

### 3.1 POST `/ai/news/generate` - 팀 뉴스 생성

**인증**: Client Credentials

**Request Body**
```json
{
  "teamId": 3,
  "teamName": "전북 현대 모터스",
  "sportName": "축구",
  "date": "2026-02-14"
}
```

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "뉴스가 생성되었습니다.",
  "data": {
    "title": "[AI 뉴스] 전북 현대 모터스 2월 14일 소식",
    "content": "전북 현대 모터스의 최신 소식을 전해드립니다...",
    "postId": 500
  }
}
```

> 스케줄러에 의해 매일 호출. 생성된 뉴스는 해당 팀의 NEWS 게시판에 자동 등록.

---

### 3.2 POST `/ai/comments/generate` - AI 댓글 생성

**인증**: Client Credentials

> Kafka Consumer `qna-post.created` 이벤트에 의해 트리거

**Request Body**
```json
{
  "postId": 123,
  "title": "축구 오프사이드 규칙이 뭔가요?",
  "content": "축구를 보기 시작했는데 오프사이드 규칙이 이해가 안 됩니다."
}
```

**Response 200**
```json
{
  "code": "SUCCESS",
  "message": "AI 댓글이 생성되었습니다.",
  "data": {
    "commentId": 601,
    "content": "오프사이드는 공격 선수가 상대 진영에서 공보다 앞에 있을 때..."
  }
}
```

> AI가 생성한 댓글은 `is_ai_generated=true`로 저장. resource-service의 댓글 작성 API를 Client Credentials로 호출하여 저장.

---

## 부록: 에러 코드 전체 목록

| 도메인 | 코드 | HTTP 상태 | 설명 |
|--------|------|-----------|------|
| AUTH | AUTH_INVALID_CREDENTIALS | 401 | 잘못된 인증 정보 |
| AUTH | AUTH_TOKEN_EXPIRED | 401 | 토큰 만료 |
| AUTH | AUTH_TOKEN_INVALID | 401 | 유효하지 않은 토큰 |
| AUTH | AUTH_ACCESS_DENIED | 403 | 권한 없음 |
| USER | USER_NOT_FOUND | 404 | 사용자 없음 |
| USER | USER_SUSPENDED | 403 | 정지된 사용자 |
| USER | USER_EMAIL_DUPLICATED | 409 | 이메일 중복 |
| USER | USER_NICKNAME_DUPLICATED | 409 | 닉네임 중복 |
| POST | POST_NOT_FOUND | 404 | 게시글 없음 |
| POST | POST_ACCESS_DENIED | 403 | 게시글 접근 권한 없음 |
| POST | POST_ALREADY_REPORTED | 409 | 이미 신고한 게시글 |
| COMMENT | COMMENT_NOT_FOUND | 404 | 댓글 없음 |
| COMMENT | COMMENT_REPLY_DEPTH_EXCEEDED | 400 | 대댓글 depth 초과 |
| FILE | FILE_SIZE_EXCEEDED | 400 | 파일 크기 초과 |
| FILE | FILE_TYPE_NOT_ALLOWED | 400 | 허용되지 않은 파일 타입 |
| FILE | FILE_COUNT_EXCEEDED | 400 | 첨부파일 개수 초과 |
| COMMON | COMMON_INVALID_PARAMETER | 400 | 잘못된 요청 파라미터 |
| COMMON | COMMON_RATE_LIMIT_EXCEEDED | 429 | Rate Limit 초과 |
| COMMON | COMMON_DUPLICATE_REQUEST | 409 | 중복 요청 (멱등키) |
| COMMON | COMMON_INTERNAL_ERROR | 500 | 서버 내부 오류 |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-15 | - | 초안 작성 |
