# Resource Service 구현 계획

> 기반 문서: `sds.md`, `srs.md`, `api.md`, `db_sds.md`
> 현재 상태: Spring Boot 4.0.2 / Maven / Java 17 스켈레톤 프로젝트

---

## 현재 프로젝트 상태

| 항목 | 상태 |
|------|------|
| 프레임워크 | Spring Boot 4.0.2 (Maven, Java 17) |
| 현재 코드 | `ResourceServiceApplication.java`, `InfoController.java` (api/name, api/version) |
| 의존성 | JPA, Validation, WebMVC, H2, MySQL, Lombok |
| 누락 의존성 | MapStruct, SpringDoc(Swagger), Redis, Kafka, AWS S3 SDK, MariaDB Connector/J |

## 구현 범위 요약

| 항목 | 수량 |
|------|------|
| API 엔드포인트 | 46개 |
| DB 테이블 (Entity) | 17개 |
| Repository | 14개 |
| Service | 12개 |
| Controller | 11개 |
| Enum | 12개 |
| Kafka Producer 토픽 | 5개 |

---

## Phase 1: 프로젝트 기반 구조

### 1.1 의존성 추가 (pom.xml)
- [x] MapStruct (+ lombok-mapstruct-binding)
- [x] SpringDoc OpenAPI (Swagger UI)
- [x] Spring Data Redis
- [x] Spring Kafka
- [x] AWS S3 SDK
- [x] MariaDB Connector/J (SDS 확정, 현재 MySQL connector → MariaDB Connector/J로 교체 필요)
- [x] Spring Security
- [x] Spring Boot Actuator (헬스체크, 메트릭, 로그 레벨 제어)

### 1.2 패키지 구조 생성
```
com.lockerroom.resourceservice/
├── configuration/
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   ├── WebMvcConfig.java
│   ├── MessageSourceConfig.java
│   ├── RedisConfig.java
│   ├── KafkaProducerConfig.java
│   └── S3Config.java
├── controller/
├── service/
│   └── impl/
├── repository/
├── model/
│   ├── entity/
│   └── enums/
├── dto/
│   ├── request/
│   └── response/
├── mapper/
├── exceptions/
├── security/
├── kafka/
│   └── event/
└── utils/
```

### 1.3 공통 컴포넌트
- [x] `BaseEntity.java` — createdAt, updatedAt, deletedAt, softDelete(), isDeleted()
- [x] `ApiResponse<T>` — success(data), success(message, data), error(code, message)
- [x] `CursorPageResponse<T>` — items, nextCursor, hasNext
- [x] `CursorPageRequest` — cursor, size(기본20, 최대100), sort
- [x] `ErrorCode` enum — 20개 에러 코드 (AUTH 4개, USER 4개, POST 3개, COMMENT 2개, FILE 3개, COMMON 4개)
- [x] `CustomException` extends RuntimeException
- [x] `FieldError` record — 유효성 검증 에러용 (field, message)
- [x] `GlobalExceptionHandler` — @RestControllerAdvice (CustomException, MethodArgumentNotValidException, Exception)
- [x] `MessageUtils.java` — i18n 메시지 조회 유틸
- [x] `Constants.java` — 상수 정의
- [x] `ResourceServiceApplication.java` — @EnableJpaAuditing 추가

### 1.4 설정 파일
- [x] `SwaggerConfig` — OpenAPI 3.0 + JWT Bearer 인증 스키마
- [x] `WebMvcConfig` — CORS 설정 (localhost:3000, Idempotency-Key)
- [x] `SecurityConfig` — CSRF 비활성, Stateless, 전체 permitAll (Phase 8에서 JWT 추가)
- [x] `MessageSourceConfig` — 다국어 메시지 소스
- [x] `application.yaml` — 전체 설정 (MariaDB, Redis, Kafka, S3, JWT, CORS, SpringDoc)
- [x] `application-local.yaml` — H2 인메모리, Redis auto-config 제외, ddl-auto: create-drop
- [x] `exceptions/exceptions_ko.properties` — 에러 메시지 한국어
- [x] `messages/messages_ko.properties` — 일반 메시지 한국어
- [x] `validations/validations_ko.properties` — 검증 메시지 한국어

---

## Phase 2: Enum 클래스 (12개)

- [x] `Role` — USER, ADMIN
- [x] `OAuthProvider` — GOOGLE, KAKAO, NAVER
- [x] `BoardType` — TEAM, COMMON, QNA, NOTICE, NEWS
- [x] `ReportStatus` — PENDING, APPROVED, REJECTED
- [x] `InquiryType` — GENERAL, BUG, SUGGESTION
- [x] `InquiryStatus` — PENDING, ANSWERED
- [x] `RequestType` — SPORT, TEAM
- [x] `RequestStatus` — PENDING, APPROVED, REJECTED
- [x] `NotificationType` — COMMENT, REPLY, NOTICE, INQUIRY_REPLY
- [x] `TargetType` — POST, COMMENT, INQUIRY, NOTICE (파일/알림 통합)
- [x] `NoticeScope` — ALL, TEAM
- [x] `SearchType` — TITLE, CONTENT, TITLE_CONTENT, NICKNAME

---

## Phase 3: JPA Entity (17개 테이블)

### 3.1 사용자 도메인
- [x] `User` — id, email, password, nickname, role, provider, providerId, extends BaseEntity
- [x] `UserTeam` — id, user, team, sport (@ManyToOne), extends BaseEntity
- [x] `UserSuspension` — id, user, admin (@ManyToOne), reason, suspendedAt, suspendedUntil, extends BaseEntity
- [x] `UserWithdrawal` — id, userId(Long, FK 없음), email, reason, withdrawnAt, extends BaseEntity

### 3.2 종목/팀 도메인
- [x] `Sport` — id, name, isActive, extends BaseEntity
- [x] `Team` — id, sport (@ManyToOne), name, logoUrl, isActive, extends BaseEntity

### 3.3 게시판/게시글 도메인
- [x] `Board` — id, name, type, team (@ManyToOne nullable), extends BaseEntity
- [x] `Post` — id, board, user (@ManyToOne), title, content, viewCount, likeCount, commentCount, isAiGenerated, extends BaseEntity
- [x] `PostLike` — id, post, user (@ManyToOne), extends BaseEntity
- [x] `PostReport` — id, post, user, admin (@ManyToOne), reason, status, processedAt, extends BaseEntity

### 3.4 댓글 도메인
- [x] `Comment` — id, post, user, parent (@ManyToOne self-ref), content, isAiGenerated, extends BaseEntity

### 3.5 공지사항 도메인
- [x] `Notice` — id, title, content, isPinned, scope, team, admin (@ManyToOne), extends BaseEntity

### 3.6 고객센터 도메인
- [x] `Inquiry` — id, user (@ManyToOne), type, title, content, status, extends BaseEntity
- [x] `InquiryReply` — id, inquiry, admin (@ManyToOne), content, extends BaseEntity

### 3.7 요청 도메인
- [x] `Request` — id, user, admin (@ManyToOne), type, name, reason, status, processedAt, rejectReason, extends BaseEntity

### 3.8 파일 도메인
- [x] `FileEntity` — id, user (@ManyToOne), targetType, targetId (다형성), originalName, storedName, s3Path, size, mimeType, extends BaseEntity

### 3.9 알림 도메인
- [x] `Notification` — id, user (@ManyToOne), type, targetType, targetId (다형성), message, isRead, readAt, extends BaseEntity

---

## Phase 4: Repository 인터페이스 (14개)

- [x] `UserRepository` — findByEmail, existsByEmail, existsByNickname
- [x] `UserTeamRepository` — findByUserId, existsByUserIdAndTeamId
- [x] `UserSuspensionRepository` — findActiveByUserId (커스텀 JPQL, suspendedUntil > now)
- [x] `UserWithdrawalRepository`
- [x] `SportRepository` — findByIsActiveTrue
- [x] `TeamRepository` — findBySportIdAndIsActiveTrue
- [x] `BoardRepository` — findByTypeIn, findByTeamId
- [x] `PostRepository` — searchByBoard (커스텀 JPQL, SearchType 동적 검색), findByUserId
- [x] `PostLikeRepository` — findByPostIdAndUserId, existsByPostIdAndUserId, countByPostId
- [x] `PostReportRepository` — existsByPostIdAndUserId, findByStatus
- [x] `CommentRepository` — findByPostIdAndParentIsNull, findByParentId, findByUserId, countByPostId
- [x] `NoticeRepository` — findByDeletedAtIsNullOrderByIsPinnedDescCreatedAtDesc
- [x] `InquiryRepository` — findByUserId
- [x] `InquiryReplyRepository` — findByInquiryIdOrderByCreatedAtAsc
- [x] `RequestRepository` — findByUserId
- [x] `FileRepository` — findByTargetTypeAndTargetIdAndDeletedAtIsNull
- [x] `NotificationRepository` — findByUserId, countByUserIdAndIsReadFalse, markAllAsReadByUserId

---

## Phase 5: DTO (Request / Response)

### 5.1 사용자
- [x] `UserResponse` — id, email, nickname, role, provider, teams[], createdAt
- [x] `UserUpdateRequest` — nickname, currentPassword, newPassword (record + validation)
- [x] `WithdrawRequest` — reason, password (record)
- [x] `UserPostListResponse` — id, boardId, boardName, title, viewCount, likeCount, commentCount, createdAt
- [x] `UserCommentListResponse` — id, postId, postTitle, content, createdAt
- [x] `UserLikeListResponse` — id, boardId, boardName, title, authorNickname, viewCount, likeCount, commentCount, createdAt

### 5.2 종목/팀
- [x] `SportResponse` — id, name, isActive
- [x] `TeamResponse` — id, name, logoUrl, isActive

### 5.3 게시판
- [x] `BoardResponse` — id, name, type, teamId, teamName

### 5.4 게시글
- [x] `PostCreateRequest` — boardId, title, content, fileIds (record + validation)
- [x] `PostUpdateRequest` — title, content, fileIds (record + validation)
- [x] `PostListResponse` — id, title, authorNickname, viewCount, likeCount, commentCount, isAiGenerated, createdAt
- [x] `PostDetailResponse` — id, boardId, boardName, author(AuthorInfo), title, content, viewCount, likeCount, commentCount, isAiGenerated, isLiked, files(FileResponse[]), createdAt, updatedAt
- [x] `LikeResponse` — postId, isLiked, likeCount
- [x] `ReportRequest` — reason (record + validation)
- [x] `ReportResponse` — reportId, postId, status

### 5.5 댓글
- [x] `CommentCreateRequest` — content (record + validation)
- [x] `CommentUpdateRequest` — content (record + validation)
- [x] `CommentResponse` — id, author(AuthorInfo), content, isAiGenerated, createdAt, replies(CommentResponse[])

### 5.6 공지사항
- [x] `NoticeListResponse` — id, title, isPinned, scope, teamName, createdAt
- [x] `NoticeDetailResponse` — id, title, content, isPinned, scope, teamId, teamName, adminNickname, createdAt, updatedAt

### 5.7 고객센터
- [x] `InquiryCreateRequest` — type, title, content, fileIds (record + validation)
- [x] `InquiryListResponse` — id, type, title, status, createdAt
- [x] `InquiryDetailResponse` — id, type, title, content, status, files(FileResponse[]), replies(InquiryReplyResponse[]), createdAt

### 5.8 요청
- [x] `RequestCreateRequest` — type, name, reason (record + validation)
- [x] `RequestListResponse` — id, type, name, status, createdAt
- [x] `RequestDetailResponse` — id, type, name, reason, status, rejectReason, processedAt, createdAt

### 5.9 알림
- [x] `NotificationResponse` — id, type, targetType, targetId, message, isRead, readAt, createdAt
- [x] `UnreadCountResponse` — unreadCount

### 5.10 파일
- [x] `FileResponse` — id, originalName, url, size, mimeType

### 5.11 관리자
- [x] `AdminUserListResponse` — id, email, nickname, role, provider, isSuspended, createdAt
- [x] `SuspendRequest` — reason, suspendedUntil(@Future) (record + validation)
- [x] `ReportListResponse` — id, postId, postTitle, reporterNickname, reason, status, createdAt
- [x] `ReportProcessRequest` — status, action (record + validation)
- [x] `NoticeCreateRequest` — title, content, isPinned, scope, teamId (record + validation)
- [x] `AdminInquiryListResponse` — id, userNickname, type, title, status, createdAt
- [x] `InquiryReplyRequest` — content (record + validation)
- [x] `AdminRequestListResponse` — id, userNickname, type, name, reason, status, createdAt
- [x] `RequestProcessRequest` — status, rejectReason (record + validation)

### 5.extra 공유 타입 (계획 외 추가)
- [x] `AuthorInfo` — id, nickname (PostDetailResponse, CommentResponse 공유)
- [x] `UserTeamInfo` — teamId, teamName, sportId, sportName (UserResponse 내 teams[])
- [x] `InquiryReplyResponse` — id, adminNickname, content, createdAt (InquiryDetailResponse 내 replies[])

### 5.12 MapStruct Mapper
- [x] `UserMapper`, `PostMapper`, `CommentMapper`, `NoticeMapper`, `InquiryMapper`, `RequestMapper`, `NotificationMapper`, `FileMapper`

---

## Phase 6: Service 레이어 (Interface + Impl)

| Service | 주요 메서드 | 비고 | 상태 |
|---------|------------|------|------|
| UserService | getMyInfo, updateMyInfo, withdraw, getMyPosts, getMyComments, getMyLikes | | [x] |
| SportService | getSports, getTeamsBySport | 캐싱 고려 (Phase 8) | [x] |
| BoardService | getBoards, validateBoardAccess, getPostsByBoard | 팀 전용 접근제어 | [x] |
| PostService | create, getDetail, update, delete, toggleLike, report | Kafka 연동 (Phase 8) | [x] |
| CommentService | create, createReply, update, delete, getByPost | depth 1 제한, Kafka 알림 (Phase 8) | [x] |
| NoticeService | getList, getDetail | isPinned 우선 정렬 | [x] |
| InquiryService | create, getMyList, getDetail | | [x] |
| RequestService | create, getMyList, getDetail | | [x] |
| NotificationService | getMyList, getUnreadCount, markAsRead, markAllAsRead | | [x] |
| FileService | upload, delete, getFilesByTarget | S3 placeholder (Phase 8) | [x] |
| IdempotencyService | isDuplicate, getExistingResponse, saveResponse | in-memory placeholder (Phase 8 Redis) | [x] |
| AdminService | getUsers, suspendUser, getReports, processReport, createNotice, updateNotice, deleteNotice, getInquiries, replyInquiry, getRequests, processRequest | | [x] |

### 6.extra 계획 외 추가 구현
- [x] `User.updateNickname()`, `User.updatePassword()` — 엔티티 도메인 메서드 추가
- [x] `PostLikeRepository.findByUserIdWithPost()` — JOIN FETCH 좋아요 목록 조회
- [x] `UserRepository.findByDeletedAtIsNullOrderByIdDesc()` — 관리자 사용자 목록 (커서 지원)
- [x] `InquiryRepository.findByDeletedAtIsNullOrderByIdDesc()` — 관리자 문의 목록 (커서 지원)
- [x] `RequestRepository.findByDeletedAtIsNullOrderByIdDesc()` — 관리자 요청 목록 (커서 지원)
- [x] `ErrorCode` 19개 추가 — BOARD, NOTICE, INQUIRY, REQUEST, NOTIFICATION, REPORT, SPORT, DUPLICATE 카테고리

---

## Phase 7: Controller 레이어 (11개, 46 엔드포인트)

### 7.1 UserController (`/api/v1/users`) — [x]
| Method | URI | 설명 |
|--------|-----|------|
| GET | /users/me | 내 정보 조회 |
| PUT | /users/me | 내 정보 수정 |
| DELETE | /users/me | 회원 탈퇴 |
| GET | /users/me/posts | 내가 쓴 글 |
| GET | /users/me/comments | 내가 쓴 댓글 |
| GET | /users/me/likes | 좋아요한 글 |

### 7.2 SportController (`/api/v1/sports`) — [x]
| Method | URI | 설명 |
|--------|-----|------|
| GET | /sports | 종목 목록 |
| GET | /sports/{sportId}/teams | 팀 목록 |

### 7.3 BoardController (`/api/v1/boards`) — [x]
| Method | URI | 설명 |
|--------|-----|------|
| GET | /boards | 게시판 목록 |
| GET | /boards/{boardId}/posts | 게시판별 게시글 목록 |

### 7.4 PostController (`/api/v1/posts`) — [x]
| Method | URI | 설명 |
|--------|-----|------|
| POST | /posts | 게시글 작성 |
| GET | /posts/{postId} | 게시글 상세 |
| PUT | /posts/{postId} | 게시글 수정 |
| DELETE | /posts/{postId} | 게시글 삭제 |
| POST | /posts/{postId}/like | 좋아요 토글 |
| POST | /posts/{postId}/report | 게시글 신고 |

### 7.5 CommentController — [x]
| Method | URI | 설명 |
|--------|-----|------|
| GET | /posts/{postId}/comments | 댓글 목록 |
| POST | /posts/{postId}/comments | 댓글 작성 |
| PUT | /comments/{commentId} | 댓글 수정 |
| DELETE | /comments/{commentId} | 댓글 삭제 |
| POST | /comments/{commentId}/replies | 대댓글 작성 |

### 7.6 NoticeController (`/api/v1/notices`) — [x]
| Method | URI | 설명 |
|--------|-----|------|
| GET | /notices | 공지 목록 |
| GET | /notices/{noticeId} | 공지 상세 |

### 7.7 InquiryController (`/api/v1/inquiries`) — [x]
| Method | URI | 설명 |
|--------|-----|------|
| POST | /inquiries | 문의 작성 |
| GET | /inquiries | 내 문의 목록 |
| GET | /inquiries/{inquiryId} | 문의 상세 |

### 7.8 RequestController (`/api/v1/requests`) — [x]
| Method | URI | 설명 |
|--------|-----|------|
| POST | /requests | 요청 작성 |
| GET | /requests | 내 요청 목록 |
| GET | /requests/{requestId} | 요청 상세 |

### 7.9 NotificationController (`/api/v1/notifications`) — [x]
| Method | URI | 설명 |
|--------|-----|------|
| GET | /notifications | 알림 목록 |
| GET | /notifications/unread-count | 미읽음 수 |
| PUT | /notifications/{notificationId}/read | 읽음 처리 |
| PUT | /notifications/read-all | 전체 읽음 |

### 7.10 FileController (`/api/v1/files`) — [x]
| Method | URI | 설명 |
|--------|-----|------|
| POST | /files | 파일 업로드 |
| DELETE | /files/{fileId} | 파일 삭제 |

### 7.11 AdminController (`/api/v1/admin`) — [x]
| Method | URI | 설명 |
|--------|-----|------|
| GET | /admin/users | 회원 목록 |
| PUT | /admin/users/{userId}/suspend | 회원 정지 |
| GET | /admin/reports | 신고 목록 |
| PUT | /admin/reports/{reportId} | 신고 처리 |
| POST | /admin/notices | 공지 작성 |
| PUT | /admin/notices/{noticeId} | 공지 수정 |
| DELETE | /admin/notices/{noticeId} | 공지 삭제 |
| GET | /admin/inquiries | 문의 목록 |
| POST | /admin/inquiries/{inquiryId}/reply | 문의 답변 |
| GET | /admin/requests | 요청 목록 |
| PUT | /admin/requests/{requestId} | 요청 처리 |

### 7.extra 공통 사항
- 인증: `@CurrentUserId` 커스텀 어노테이션 — JWT `sub` 클레임(Keycloak UUID)에서 사용자 식별
- 응답 래퍼: 모든 응답 `ApiResponse<T>`로 통일
- 유효성 검증: `@Valid @RequestBody`로 Jakarta Validation 적용
- 리소스 생성: POST 엔드포인트는 `HttpStatus.CREATED` (201) 반환
- CommentController: 대댓글 API(`/comments/{commentId}/replies`)에서 postId를 parent comment로부터 자동 추출

---

## Phase 8: 외부 연동 ✅

### 8.1 Security (Keycloak JWT OAuth2 Resource Server)
- [x] `SecurityConfig` — OAuth2 Resource Server JWT + Keycloak role converter + `/api/v1/admin/**` ROLE_ADMIN 인가
- [x] `KeycloakRoleConverter` — JWT `realm_access.roles` → Spring Security `GrantedAuthority` 변환
- [x] `@CurrentUserId` 커스텀 어노테이션 + `CurrentUserIdArgumentResolver` — JWT `sub` → `User.keycloakId` → `userId` 해석
- [x] `User.keycloakId` 필드 추가 (VARCHAR(36), UNIQUE, nullable)

### 8.2 Redis
- [x] `RedisConfig` — StringRedisTemplate 빈 설정
- [x] `IdempotencyServiceImpl` — Redis 기반 (24h TTL) + 인메모리 폴백 (local 프로파일)

### 8.3 Kafka Producer
- [x] `KafkaProducerService` — send(topic, key, event) + KafkaTemplate null 시 graceful no-op
- [x] 이벤트 DTO: `QnaPostCreatedEvent`, `NotificationEvent` (Java record)
- [x] 5개 토픽 연동:
  - `qna-post.created` — PostServiceImpl (QNA 게시판 글 작성 시)
  - `notification.comment` — CommentServiceImpl (작성자 ≠ 게시글 작성자)
  - `notification.reply` — CommentServiceImpl (작성자 ≠ 부모 댓글 작성자)
  - `notification.inquiry-replied` — AdminServiceImpl (문의 답변 시)
  - `notification.report-processed` — AdminServiceImpl (신고 처리 시)
- [x] `Constants.java` — 토픽명 상수 5개 + 멱등성 Redis 키 접두사/TTL 상수 추가

### 8.4 AWS S3
- [x] `S3Config` — AWS SDK v2 S3Client 빈 (`@Profile("!local")`)
- [x] `FileServiceImpl` — S3 업로드(PutObject)/삭제(DeleteObject) 구현 + S3Client null 시 graceful 폴백

### 8.5 로컬 프로파일 정리
- [x] `application-local.yaml` — KafkaAutoConfiguration 제외 추가
- [x] `NotificationType` — REPORT_PROCESSED 추가

---

## Phase 9: Postman 컬렉션 ✅
- [x] `locker-room-resource` 컬렉션 생성 — 46개 API, 11개 폴더
- [x] 컬렉션 변수: baseUrl, userId, adminId, userRole, postId, commentId, boardId, sportId, noticeId, inquiryId, requestId, notificationId, fileId, reportId
- [x] 파일 위치: `postman/locker-room-resource.postman_collection.json`

---

## Phase 10: 테스트 ✅

| 레이어 | 대상 | 도구 | 테스트 수 | 상태 |
|--------|------|------|:---------:|:----:|
| Unit | Service (12개) | JUnit 5 + Mockito | 138 | [x] |
| Unit | Security (1개) | JUnit 5 + Mockito | 5 | [x] |
| Controller | Controller (4개) | @WebMvcTest + MockMvc | 39 | [x] |
| Integration | Repository (5개) | @DataJpaTest + H2 | 64 | [x] |
| **합계** | **22개 파일** | | **248** (1 skipped) | **PASS** |

---

## Phase 11: 코드 리뷰 기반 보완

> 문서(SRS, SDS, API 명세, DB 스키마)와 실제 구현 코드를 대조 분석한 결과.
> 리뷰 일시: 2026-02-25

### 11.1 [높음] 멱등성(Idempotency) Controller 적용 ✅

> ~~`IdempotencyServiceImpl` 구현 완료됐으나, **어떤 Controller에서도 호출되지 않음.**~~
> 완료: `@Idempotent` 커스텀 어노테이션 + AOP Aspect 방식으로 구현.
> 사용자별 복합 키(`sub:idempotencyKey`) 로 교차 충돌 방지.

- [x] `@Idempotent` 커스텀 어노테이션 생성 (`aop/Idempotent.java`)
- [x] `IdempotencyAspect` AOP 구현 — `Idempotency-Key` 헤더 검증, 중복 시 캐시 응답 반환, 성공 시 응답 저장 (`aop/IdempotencyAspect.java`)
- [x] `ErrorCode.IDEMPOTENCY_KEY_MISSING` 추가 (400 Bad Request)
- [x] 7개 POST 엔드포인트에 `@Idempotent` 적용: `PostController.create/toggleLike/report`, `CommentController.create/createReply`, `InquiryController.create`, `RequestController.create`
- [x] `spring-boot-starter-aop` 의존성 추가

### 11.2 [높음] Cursor 기반 페이지네이션 실제 구현 ✅

> ~~현재 모든 페이지네이션이 `PageRequest.of(0, size+1)`로 **항상 첫 페이지만 조회**.~~
> 완료: ID 기반 cursor 페이지네이션으로 전환. Base64 인코딩/디코딩 적용.

- [x] Repository 쿼리에 cursor 조건 추가 (`WHERE id < :cursor ORDER BY id DESC`)
- [x] `CursorPageRequest.decodeCursor()` — Base64 → Long ID 디코딩
- [x] `CursorPageRequest.encodeCursor(Long id)` — Long ID → Base64 인코딩
- [x] 8개 Repository 커서 지원 메서드 추가 (IdLessThan 파생 쿼리 + JPQL cursor 파라미터)
- [x] 6개 Service 구현체 커서 로직 적용: `UserServiceImpl`, `BoardServiceImpl`, `AdminServiceImpl`, `NotificationServiceImpl`, `InquiryServiceImpl`, `RequestServiceImpl`
- [x] 8개 테스트 파일 업데이트 (서비스 6개 + 레포지토리 2개)
- [x] 전체 248개 테스트 통과 확인
- ~~`sort` 파라미터 반영~~ — 현재 모든 목록이 `id DESC` 정렬로 통일 (추후 필요 시 확장)

### 11.3 [높음] 파일(File) 처리 로직 보완 ✅

> ~~파일 업로드/삭제 기본 기능은 있으나, 게시글과의 연결/해제 및 검증 대부분 누락.~~
> 완료: 파일 연결/검증/삭제 전면 보강. 물리 삭제(S3+DB hard delete) 적용.

#### 11.3.1 파일 ↔ 게시글 연결
- [x] `PostServiceImpl.create()` — `fileService.linkFilesToTarget()` 호출로 `FileEntity.targetId` 업데이트
- [x] `PostServiceImpl.update()` — `syncFiles()` 헬퍼로 기존 vs 새 fileIds 비교, 제거분 S3+DB 삭제, 신규분 연결
- [x] `PostServiceImpl.delete()` — `fileService.deleteFilesByTarget()` 호출로 첨부파일 물리 삭제
- [x] `InquiryServiceImpl.create()` — `fileService.linkFilesToTarget()` 호출로 문의 파일 연결

#### 11.3.2 파일 검증 강화
- [x] `FileController.upload()` — `targetType` 요청 파라미터 추가 (POST, INQUIRY, COMMENT 선택)
- [x] MIME 타입 허용 목록 검증: 이미지(jpeg/png/gif/webp), 일반(pdf/txt) — `Constants.ALLOWED_MIME_TYPES`
- [x] MIME 타입 이중 검증: Content-Type 헤더 + 파일 매직넘버 (JPEG/PNG/GIF/WEBP/PDF 시그니처)
- [x] 파일 크기 분리: 이미지 10MB (`MAX_IMAGE_FILE_SIZE`) / 일반 20MB (`MAX_DOCUMENT_FILE_SIZE`)
- [x] 파일 개수 제한: `linkFilesToTarget()` 에서 건당 최대 5개 검증 (`Constants.MAX_FILE_COUNT`)
- [x] 파일 삭제 방식: Soft Delete → Hard Delete(S3 + DB) 전환 (`RES-FILE-006`)

### 11.4 [높음] 관리자 기능 보완 ✅

> ~~관리자 API 기본 CRUD는 있으나, 필터링과 자동 생성 로직 누락.~~
> 완료: 4개 목록 API 필터링, 요청 승인 자동 생성, 신고 action 세분화 구현.

#### 11.4.1 필터 파라미터 구현
- [x] `GET /admin/users` — `keyword` (닉네임/이메일 LIKE 검색), `role` (USER/ADMIN) 필터 (JPQL NULL 체크 패턴)
- [x] `GET /admin/reports` — `status` (PENDING/APPROVED/REJECTED) 옵셔널 필터 (PENDING 하드코딩 제거)
- [x] `GET /admin/inquiries` — `status` (PENDING/ANSWERED), `type` (GENERAL/BUG/SUGGESTION) 필터
- [x] `GET /admin/requests` — `status` (PENDING/APPROVED/REJECTED), `type` (SPORT/TEAM) 필터
- [x] 공통: `buildCursorPage()` 헬퍼로 페이지네이션 중복 코드 제거

#### 11.4.2 요청 승인 시 자동 생성
- [x] `AdminServiceImpl.processRequest()` — APPROVED 시 `autoCreateFromRequest()` 호출
- [x] APPROVED + SPORT 요청: `Sport` 자동 생성 (request.name 사용)
- [x] APPROVED + TEAM 요청: `Team` + `Board`(TEAM) + `Board`(NEWS) 자동 생성 (RequestProcessRequest에 `sportId` 필드 추가)

#### 11.4.3 신고 처리 action 보완
- [x] `action` 값을 `"DELETE_POST"`, `"SUSPEND_USER"`로 분리 (`handleReportAction()`)
- [x] `"SUSPEND_USER"` action: 게시글 작성자 UserSuspension 생성 + 게시글 soft delete
- [x] `ReportProcessRequest`에 `suspensionDays` 필드 추가 (미지정 시 기본 30일)

---

### 11.5 [중간] 비즈니스 로직 불일치 수정 ✅ (완료)

#### 11.5.1 게시글/댓글 삭제 시 관리자 권한 허용
- [x] `PostServiceImpl.validatePostOwner()` — ADMIN 역할도 삭제 가능하도록 수정 (`RES-POST-007`)
  - 본인이 아닌 경우 User를 조회하여 `Role.ADMIN`이면 허용
- [x] `CommentServiceImpl.validateCommentOwner()` — ADMIN 역할도 삭제 가능하도록 수정
  - 동일한 패턴 적용

#### 11.5.2 비밀번호 검증 추가
- [x] `UserServiceImpl.updateMyInfo()` — `currentPassword` 검증 후 비밀번호 변경 허용 (`RES-USER-002`)
  - `newPassword` 전달 시 `currentPassword`가 기존 비밀번호와 일치하지 않으면 `INVALID_PASSWORD` 예외
- [x] `UserServiceImpl.withdraw()` — `password` 본인 확인 검증 추가
  - `password`가 기존 비밀번호와 일치하지 않으면 `INVALID_PASSWORD` 예외

#### 11.5.3 공지사항 teamId 필터
- [x] `GET /notices` — `teamId` 쿼리 파라미터 수신 추가 (`NoticeController`)
- [x] `NoticeServiceImpl.getList()` — teamId 기반 필터링 로직 구현
  - `NoticeRepository.findFilteredNotices()` JPQL 메서드 추가 (`teamId IS NULL` 시 전체 반환)

#### 11.5.4 전체 알림 읽음 응답
- [x] `NotificationController.markAllAsRead()` — 갱신 건수 반환 (`{ "updatedCount": N }`)
  - `MarkAllReadResponse` record 생성, `ApiResponse.success(new MarkAllReadResponse(updatedCount))` 반환
- [x] `NotificationRepository.markAllAsReadByUserId()` — 이미 `int` 반환 (변경 불필요)
- [x] `NotificationService.markAllAsRead()` 반환 타입 `void` → `int` 변경

#### 11.5.5 게시판 목록 NOTICE/NEWS 포함
- [x] `BoardServiceImpl.getBoards()` — NOTICE, NEWS 타입 게시판도 공개 목록에 포함
  - `findByTypeIn(List.of(COMMON, QNA, NOTICE, NEWS))`

#### 11.5.6 댓글 목록 Cursor 페이지네이션
- [x] `CommentController.getByPost()` — `CursorPageResponse<CommentResponse>` 반환으로 변경
  - `@ModelAttribute CursorPageRequest` 파라미터 추가
- [x] `CommentServiceImpl.getByPost()` — Cursor 기반 페이지네이션 적용
  - 루트 댓글만 커서 페이지네이션, 각 루트 댓글의 대댓글은 전체 조회
  - 댓글은 오래된 순(ASC) 정렬, `idGreaterThan` 커서 방식
  - `CommentRepository` 기존 `OrderByCreatedAtAsc` → `OrderByIdAsc` + `Pageable` 메서드로 변경
- [x] 관련 테스트(`CommentControllerTest`, `CommentServiceImplTest`, `NoticeServiceImplTest`, `NotificationServiceImplTest`) 동기화 완료

---

### 11.6 [낮음] 코드 품질 및 명세 정합성

#### 11.6.1 ErrorCode 형식 통일
- [ ] ErrorCode `code` 값을 SDS 명세 형식으로 변경 (`USER_001` → `USER_NOT_FOUND` 등)
- [ ] `exceptions_ko.properties` 키도 함께 변경
- [ ] 누락 에러코드 추가: `FILE_TYPE_NOT_ALLOWED`, `FILE_COUNT_EXCEEDED`, `COMMON_RATE_LIMIT_EXCEEDED`, `COMMON_DUPLICATE_REQUEST`

#### 11.6.2 DTO 유효성 검증 보강
- [ ] `CommentCreateRequest.content` — `@Size(max = 1000)` 추가
- [ ] `ReportRequest.reason` — `@Size(max = 500)` 추가
- [ ] `PostCreateRequest.content` — `@Size(max = 10000)` 추가
- [ ] `PostCreateRequest.fileIds` — `@Size(max = 5)` 추가
- [ ] `InquiryCreateRequest.content` — `@Size(max = 5000)` 추가

#### 11.6.3 Soft Delete `@SQLRestriction` 적용
- [ ] `@SQLRestriction("deleted_at IS NULL")` 또는 Hibernate `@SoftDelete` 적용 검토 (SDS 2.7)
- [ ] 적용 시 기존 수동 필터 (`.filter(p -> !p.isDeleted())`) 제거
- [ ] 삭제된 데이터 조회 필요 시 Native Query 전환

#### 11.6.4 HTTP 상태코드 정합
- [ ] `PostController.delete()` — `204 No Content` 반환으로 변경
- [ ] `CommentController.delete()` — `204 No Content` 반환으로 변경
- [ ] `AdminController.deleteNotice()` — `204 No Content` 반환으로 변경

#### 11.6.5 ApiResponse 성공 메시지 세분화
- [ ] `ApiResponse.success(T data)` 사용처에서 API별 맞춤 메시지 전달
- [ ] 예: `ApiResponse.success("게시글이 작성되었습니다.", data)` (SDS 2.3 참조)

---

### Phase 11 보완 우선순위

```
1. ✅ 11.2 Cursor 페이지네이션 — 완료 (Base64 인코딩 + ID 기반 커서)
2. ✅ 11.1 멱등성 적용 — 완료 (@Idempotent AOP + 7개 POST 엔드포인트)
3. ✅ 11.3 파일 처리 — 완료 (MIME 검증 + 매직넘버 + 크기 분리 + 연결/해제 + 물리 삭제)
4. ✅ 11.4 관리자 기능 — 완료 (필터 4개 + 자동 생성 + action 세분화)
5. ✅ 11.5 비즈니스 로직 — 완료 (ADMIN 삭제 허용, 비밀번호 검증, teamId 필터, 알림 건수, 댓글 커서 페이지네이션)
6. 11.6 코드 품질 — ErrorCode 형식, DTO 검증, 상태코드 등
```

---

## 구현 우선순위 (권장 순서)

```
1. Phase 1 (기반 구조) — 모든 것의 토대
2. Phase 2 (Enum) — Entity 의존
3. Phase 3 (Entity + Repository) — Service 의존
4. Phase 5 (DTO) — Controller/Service 의존
5. Phase 6 (Service) — 비즈니스 로직
6. Phase 7 (Controller) — API 노출
7. Phase 8 (외부 연동) — Kafka/Redis/S3
8. Phase 4 (MapStruct Mapper) — DTO ↔ Entity 변환
9. Phase 9 (Postman) — API 테스트
10. Phase 10 (테스트 코드)
11. Phase 11 (코드 리뷰 보완) — 문서 대비 미흡 사항 수정
```

---

## 결정 사항

| 항목 | 결정 | 근거 |
|------|------|------|
| 패키지명 | `com.lockerroom.resourceservice` | SDS v1.1에서 확정 |
| 빌드 도구 | Maven | SDS v1.1에서 Maven으로 변경 |
| JDBC Driver | MariaDB Connector/J | SDS v1.1에서 명시 |
| Java 버전 | 17 | 현재 프로젝트 유지 |
| 인증 방식 | Keycloak JWT (OAuth2 Resource Server) + `@CurrentUserId` | JWT `sub` 클레임 → `User.keycloakId` → `userId` |

## 미결정 사항 (TBD)

_(없음)_
