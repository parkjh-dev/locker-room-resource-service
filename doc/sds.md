# Locker Room - 소프트웨어 개발 명세서 (SDS)

> 참고 문서: `back-end/decide.md`, `back-end/api.md`
> 보일러플레이트: https://github.com/Genc/spring-boot-boilerplate

---

## 1. 개요

### 1.1 목적
본 문서는 Locker Room 백엔드 시스템의 상세 개발 명세를 정의한다. 서비스별 구현 방법, 패키지 구조, 핵심 컴포넌트, 설정 전략 등을 기술하여 개발자가 일관된 기준으로 구현할 수 있도록 한다.

### 1.2 기술 스택
| 구분 | 기술 | 버전 |
|------|------|------|
| Framework | Spring Boot | 4.0 (latest) |
| Language | Java | 17 |
| Database | MariaDB | latest |
| Cache | Redis | latest |
| Message Broker | Apache Kafka | latest |
| Object Storage | AWS S3 | - |
| Gateway | Spring Cloud Gateway | latest |
| Build Tool | Maven | latest |
| JDBC Driver | MariaDB Connector/J | latest |
| CI/CD | Jenkins | latest |
| Container | Docker | latest |
| Deployment | AWS EC2 + ECS | - |

### 1.3 서비스 구성
| 서비스 | 포트 | Context Path | 역할 |
|--------|------|--------------|------|
| gateway | 8080 | - | API Gateway, 라우팅, Rate Limiting |
| auth-service | 8081 | /api/v1/auth | 인증/인가, JWT 관리 |
| resource-service | 8082 | /api/v1 | 핵심 비즈니스 리소스 (사용자, 게시글, 댓글 등) |
| ai-service | 8083 | /api/v1/ai | AI 뉴스 생성, Q&A 자동 답변 |
| notification-service | 8084 | - | 이메일/SMS 발송 (Kafka Consumer) |

---

## 2. 공통 개발 규칙

### 2.1 패키지 구조 (서비스별 공통)

```
com.lockerroom.{servicename}/
├── LockerRoom{ServiceName}Application.java
├── configuration/
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   ├── WebMvcConfig.java
│   ├── MessageSourceConfig.java
│   ├── RedisConfig.java
│   └── KafkaConfig.java
├── controller/
│   ├── PostController.java
│   └── ...
├── service/
│   ├── PostService.java              # Interface
│   ├── impl/
│   │   └── PostServiceImpl.java      # Implementation
│   └── ...
├── repository/
│   ├── PostRepository.java
│   └── ...
├── model/
│   ├── entity/
│   │   ├── Post.java
│   │   └── ...
│   └── enums/
│       ├── BoardType.java
│       └── ...
├── dto/
│   ├── request/
│   │   ├── PostCreateRequest.java
│   │   └── ...
│   └── response/
│       ├── PostResponse.java
│       ├── PostListResponse.java
│       └── ...
├── mapper/
│   ├── PostMapper.java               # MapStruct
│   └── ...
├── exceptions/
│   ├── GlobalExceptionHandler.java   # @RestControllerAdvice
│   ├── CustomException.java
│   ├── ErrorCode.java                # Enum
│   └── ErrorResponse.java
├── security/
│   └── JwtAuthConverter.java        # Keycloak JWT → Spring Security 권한 매핑
└── utils/
    ├── MessageUtils.java
    └── Constants.java
```

> 각 서비스(resource-service, ai-service)는 `spring-boot-starter-oauth2-resource-server`를 사용하여 Keycloak JWKS 공개키로 JWT를 검증한다. `JwtAuthConverter`는 Keycloak JWT의 `realm_access.roles`를 Spring Security GrantedAuthority로 변환한다.

### 2.2 네이밍 규칙

| 구분 | 규칙 | 예시 |
|------|------|------|
| 패키지 | 소문자 | `com.lockerroom.resourceservice` |
| 클래스 | PascalCase | `PostServiceImpl` |
| 메서드 | camelCase | `findPostById()` |
| 변수 | camelCase | `commentCount` |
| 상수 | UPPER_SNAKE_CASE | `MAX_FILE_SIZE` |
| DB 컬럼 | snake_case | `created_at` |
| API URI | kebab-case | `/api/v1/users/me/posts` |
| Enum 값 | UPPER_SNAKE_CASE | `TITLE_CONTENT` |

### 2.3 공통 응답 구조

```java
@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "요청이 성공적으로 처리되었습니다.", data);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

### 2.4 페이지네이션 공통 구조

```java
@Getter
@AllArgsConstructor
public class CursorPageResponse<T> {
    private List<T> items;
    private String nextCursor;
    private boolean hasNext;
}
```

```java
@Getter
@Setter
public class CursorPageRequest {
    private String cursor;

    @Min(1) @Max(100)
    private int size = 20;

    private String sort = "created_at";
}
```

### 2.5 에러 처리

#### ErrorCode Enum
```java
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // AUTH
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_EXPIRED"),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_INVALID"),
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_ACCESS_DENIED"),

    // USER
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "USER_SUSPENDED"),
    USER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "USER_EMAIL_DUPLICATED"),
    USER_NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "USER_NICKNAME_DUPLICATED"),

    // POST
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND"),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "POST_ACCESS_DENIED"),
    POST_ALREADY_REPORTED(HttpStatus.CONFLICT, "POST_ALREADY_REPORTED"),

    // COMMENT
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND"),
    COMMENT_REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "COMMENT_REPLY_DEPTH_EXCEEDED"),

    // FILE
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE_SIZE_EXCEEDED"),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FILE_TYPE_NOT_ALLOWED"),
    FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE_COUNT_EXCEEDED"),

    // COMMON
    COMMON_INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON_INVALID_PARAMETER"),
    COMMON_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "COMMON_RATE_LIMIT_EXCEEDED"),
    COMMON_DUPLICATE_REQUEST(HttpStatus.CONFLICT, "COMMON_DUPLICATE_REQUEST"),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_ERROR");

    private final HttpStatus httpStatus;
    private final String code;
}
```

#### CustomException
```java
@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
    }
}
```

#### GlobalExceptionHandler
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        String message = MessageUtils.getMessage(errorCode.getCode());
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ApiResponse.error(errorCode.getCode(), message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, List<FieldError>>>> handleValidation(
            MethodArgumentNotValidException e) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();
        return ResponseEntity
            .badRequest()
            .body(new ApiResponse<>("COMMON_INVALID_PARAMETER",
                "입력값이 올바르지 않습니다.",
                Map.of("errors", errors)));
    }
}
```

### 2.6 i18n 메시지 관리

**디렉토리 구조**
```
src/main/resources/
├── messages/
│   ├── messages_ko.properties     # 한국어 (기본)
│   └── messages_en.properties     # 영어 (추후)
├── exceptions/
│   ├── exceptions_ko.properties
│   └── exceptions_en.properties
└── validations/
    ├── validations_ko.properties
    └── validations_en.properties
```

**예시 (exceptions_ko.properties)**
```properties
AUTH_INVALID_CREDENTIALS=이메일 또는 비밀번호가 올바르지 않습니다.
AUTH_TOKEN_EXPIRED=토큰이 만료되었습니다. 다시 로그인해주세요.
USER_NOT_FOUND=사용자를 찾을 수 없습니다.
POST_NOT_FOUND=게시글을 찾을 수 없습니다.
```

### 2.7 Soft Delete 구현

```java
@MappedSuperclass
@Getter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
```

- JPA `@Where(clause = "deleted_at IS NULL")` 또는 Hibernate `@SoftDelete` 적용
- 삭제된 데이터 조회 필요 시 Native Query 사용

### 2.8 MapStruct 변환 규칙

```java
@Mapper(componentModel = "spring")
public interface PostMapper {

    PostResponse toResponse(Post post);

    PostListResponse toListResponse(Post post);

    Post toEntity(PostCreateRequest request);
}
```

- Entity → Response DTO 변환 시 MapStruct 사용
- 복잡한 변환 로직은 `@AfterMapping` 활용
- Entity 직접 노출 절대 금지

---

## 3. Gateway Service

### 3.1 역할
- API 라우팅 (path prefix → 서비스 포워딩)
- Rate Limiting (Redis 기반)
- CORS 처리
- JWT 토큰 사전 검증 (유효성만 확인, 권한 확인은 각 서비스)

### 3.2 라우팅 설정

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/v1/auth/**

        - id: resource-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/v1/**

        - id: ai-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/v1/ai/**
```

> 라우팅 우선순위: 더 구체적인 경로가 우선 매칭 (ai > auth > resource)

### 3.3 Rate Limiting

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter:
              replenishRate: 2      # 초당 토큰 생성
              burstCapacity: 60     # 최대 토큰 수
```

| 대상 | 제한 | Key |
|------|------|-----|
| 비인증 사용자 | 60 req/min | IP |
| 인증 사용자 | 120 req/min | userId |
| 좋아요/신고 | 30 req/min | userId + path |
| 로그인/회원가입 | 10 req/min | IP |

### 3.4 CORS 설정

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Idempotency-Key"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
```

---

## 4. Auth Service

> 인증/인가는 **Keycloak(OIDC)**으로 처리한다. auth-service는 Keycloak Admin API를 활용하여 회원가입, 프로필 보완 등 비즈니스 로직을 담당하는 경량 래퍼 서비스다.

### 4.1 패키지 구조

```
com.lockerroom.authservice/
├── configuration/
│   ├── SecurityConfig.java          # OAuth2 Resource Server 설정
│   ├── KeycloakAdminConfig.java     # Keycloak Admin Client 설정
│   └── SwaggerConfig.java
├── controller/
│   └── AuthController.java          # signup, profile/complete
├── service/
│   ├── AuthService.java
│   ├── KeycloakUserService.java     # Keycloak Admin API 호출
│   ├── impl/
│   │   ├── AuthServiceImpl.java
│   │   └── KeycloakUserServiceImpl.java
├── security/
│   └── JwtAuthConverter.java        # Keycloak JWT → Spring Security 권한 매핑
├── dto/
│   ├── request/
│   │   ├── SignupRequest.java
│   │   └── ProfileCompleteRequest.java
│   └── response/
│       └── UserResponse.java
├── model/
│   ├── entity/
│   │   ├── User.java                # keycloakId 필드 포함
│   │   └── UserTeam.java
│   └── enums/
│       └── Role.java
├── repository/
│   ├── UserRepository.java
│   └── UserTeamRepository.java
├── mapper/
│   └── UserMapper.java
├── exceptions/
│   ├── GlobalExceptionHandler.java
│   ├── CustomException.java
│   └── ErrorCode.java
└── utils/
    └── MessageUtils.java
```

### 4.2 Keycloak 연동 설정

#### application.yml
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/locker-room}

keycloak:
  admin:
    server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8180}
    realm: ${KEYCLOAK_REALM:locker-room}
    client-id: ${KEYCLOAK_ADMIN_CLIENT_ID:admin-cli}
    client-secret: ${KEYCLOAK_ADMIN_CLIENT_SECRET}
```

#### SecurityConfig
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/signup").permitAll()
                .requestMatchers("/api/v1/auth/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
            );
        return http.build();
    }
}
```

#### KeycloakUserService (Keycloak Admin API 호출)
```java
@Service
@RequiredArgsConstructor
public class KeycloakUserServiceImpl implements KeycloakUserService {

    private final Keycloak keycloakAdmin;  // KeycloakAdminConfig에서 Bean 등록

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Override
    public String createUser(String email, String password) {
        UserRepresentation user = new UserRepresentation();
        user.setEmail(email);
        user.setUsername(email);
        user.setEnabled(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        Response response = keycloakAdmin.realm(realm).users().create(user);
        if (response.getStatus() == 201) {
            return CreatedResponseUtil.getCreatedId(response);  // Keycloak User ID
        }
        throw new CustomException(ErrorCode.USER_EMAIL_DUPLICATED);
    }

    @Override
    public void deleteUser(String keycloakId) {
        keycloakAdmin.realm(realm).users().get(keycloakId).remove();
    }
}
```

### 4.3 인증 흐름

```
[회원가입]
Client → POST /auth/signup
    → AuthServiceImpl.signup()
        → 이메일/닉네임 중복 검증
        → KeycloakUserService.createUser() (Keycloak Admin API)
        → User 엔티티 생성 (keycloakId 매핑)
        → UserTeam 엔티티 생성
        → 실패 시 KeycloakUserService.deleteUser() (보상 트랜잭션)
    ← UserResponse

[로그인 / 토큰 갱신 / 로그아웃]
    → 프론트엔드가 Keycloak과 직접 통신 (auth-service 미관여)
    → Authorization Code Flow + PKCE

[SSO 로그인 - 기존 회원]
    → Keycloak Identity Provider Brokering이 자동 처리
    → 프론트엔드가 Keycloak에서 토큰 수령
    → GET /users/me 호출 → 로컬 프로필 존재 → 정상 사용

[SSO 로그인 - 신규 회원 (프로필 보완)]
  Step 1) Keycloak이 IdP(Google/Kakao/Naver) 인증 + 사용자 자동 생성
  Step 2) 프론트엔드가 Keycloak에서 토큰 수령
  Step 3) GET /users/me → 404 (로컬 프로필 없음)
  Step 4) 프론트엔드: 닉네임/응원팀 입력 페이지 렌더링
  Step 5) POST /auth/profile/complete
        → AuthServiceImpl.completeProfile()
            → JWT sub 클레임으로 Keycloak 사용자 식별
            → Keycloak UserInfo에서 이메일, provider 조회
            → 닉네임 중복 검증
            → User 엔티티 생성 (keycloakId, provider 포함)
            → UserTeam 엔티티 생성
        ← UserResponse

[비밀번호 재설정]
    → Keycloak 로그인 페이지 "Forgot Password" 기능 사용
    → Keycloak이 SMTP로 재설정 이메일 직접 발송
    → auth-service 미관여
```

### 4.4 서비스 간 인증 (Client Credentials)

```
ai-service → resource-service 호출 시:
1. ai-service가 Keycloak Token Endpoint에 Client Credentials 토큰 요청
   POST /realms/{realm}/protocol/openid-connect/token
   grant_type=client_credentials&client_id=ai-service&client_secret=xxx
2. 발급받은 Access Token으로 resource-service API 호출
3. resource-service는 JWT의 azp(클라이언트)와 scope로 서비스 간 호출 판별
```

---

## 5. Resource Service

### 5.1 패키지 구조

```
com.lockerroom.resourceservice/
├── configuration/
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   ├── RedisConfig.java
│   ├── KafkaProducerConfig.java
│   ├── S3Config.java
│   └── MessageSourceConfig.java
├── controller/
│   ├── UserController.java
│   ├── SportController.java
│   ├── BoardController.java
│   ├── PostController.java
│   ├── CommentController.java
│   ├── NoticeController.java
│   ├── InquiryController.java
│   ├── RequestController.java
│   ├── NotificationController.java
│   ├── FileController.java
│   └── AdminController.java
├── service/
│   ├── UserService.java
│   ├── PostService.java
│   ├── CommentService.java
│   ├── NoticeService.java
│   ├── InquiryService.java
│   ├── RequestService.java
│   ├── NotificationService.java
│   ├── FileService.java
│   ├── IdempotencyService.java
│   ├── impl/
│   │   ├── UserServiceImpl.java
│   │   ├── PostServiceImpl.java
│   │   ├── CommentServiceImpl.java
│   │   ├── NoticeServiceImpl.java
│   │   ├── InquiryServiceImpl.java
│   │   ├── RequestServiceImpl.java
│   │   ├── NotificationServiceImpl.java
│   │   ├── FileServiceImpl.java
│   │   └── IdempotencyServiceImpl.java
├── repository/
│   ├── UserRepository.java
│   ├── PostRepository.java
│   ├── PostLikeRepository.java
│   ├── PostReportRepository.java
│   ├── CommentRepository.java
│   ├── BoardRepository.java
│   ├── SportRepository.java
│   ├── TeamRepository.java
│   ├── NoticeRepository.java
│   ├── InquiryRepository.java
│   ├── InquiryReplyRepository.java
│   ├── RequestRepository.java
│   ├── NotificationRepository.java
│   └── FileRepository.java
├── model/
│   ├── entity/
│   │   ├── BaseEntity.java
│   │   ├── User.java
│   │   ├── UserTeam.java
│   │   ├── UserSuspension.java
│   │   ├── Sport.java
│   │   ├── Team.java
│   │   ├── Board.java
│   │   ├── Post.java
│   │   ├── PostLike.java
│   │   ├── PostReport.java
│   │   ├── Comment.java
│   │   ├── Notice.java
│   │   ├── Inquiry.java
│   │   ├── InquiryReply.java
│   │   ├── Request.java
│   │   ├── Notification.java
│   │   └── File.java
│   └── enums/
│       ├── Role.java
│       ├── BoardType.java
│       ├── ReportStatus.java
│       ├── InquiryType.java
│       ├── InquiryStatus.java
│       ├── RequestType.java
│       ├── RequestStatus.java
│       ├── NotificationType.java
│       ├── TargetType.java
│       ├── NoticeScope.java
│       └── SearchType.java
├── kafka/
│   ├── KafkaProducerService.java
│   └── event/
│       ├── QnaPostCreatedEvent.java
│       ├── NotificationEvent.java
│       └── EmailEvent.java
├── dto/ ...
├── mapper/ ...
├── exceptions/ ...
├── security/ ...
└── utils/ ...
```

### 5.2 핵심 비즈니스 로직

#### 5.2.1 게시글 작성 흐름
```
PostController.createPost(PostCreateRequest, Authentication)
    → IdempotencyService.check(idempotencyKey)
    → PostServiceImpl.createPost()
        → BoardRepository.findById(boardId)
        → 게시판 접근 권한 검증 (팀 전용 게시판 체크)
        → Post 엔티티 생성 및 저장
        → fileIds가 있으면 File 엔티티의 targetId 업데이트
        → QNA 게시판이면 KafkaProducerService.send("qna-post.created", event)
    → IdempotencyService.save(idempotencyKey, response)
    ← ApiResponse<PostResponse>
```

#### 5.2.2 좋아요 토글 로직
```
PostServiceImpl.toggleLike(postId, userId)
    → PostLikeRepository.findByPostIdAndUserId()
    → 이미 좋아요:
        → PostLike soft delete
        → Post.likeCount -= 1
    → 좋아요 없음:
        → PostLike 생성
        → Post.likeCount += 1
    ← LikeResponse { postId, isLiked, likeCount }
```

#### 5.2.3 댓글 작성 + 알림 흐름
```
CommentServiceImpl.createComment(postId, request, userId)
    → PostRepository.findById(postId)
    → Comment 엔티티 생성 및 저장
    → Post.commentCount += 1
    → 게시글 작성자 ≠ 댓글 작성자이면:
        → KafkaProducerService.send("notification.comment", event)
    ← CommentResponse

CommentServiceImpl.createReply(commentId, request, userId)
    → CommentRepository.findById(commentId)
    → parent_id가 NOT NULL이면 → COMMENT_REPLY_DEPTH_EXCEEDED 예외
    → Reply 엔티티 생성 및 저장 (parent_id = commentId)
    → Post.commentCount += 1
    → 부모 댓글 작성자 ≠ 대댓글 작성자이면:
        → KafkaProducerService.send("notification.reply", event)
    ← CommentResponse
```

#### 5.2.4 게시판 접근 권한 검증
```java
private void validateBoardAccess(Board board, Long userId) {
    if (board.getType() == BoardType.TEAM) {
        if (userId == null) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }
        boolean isMember = userTeamRepository
            .existsByUserIdAndTeamId(userId, board.getTeamId());
        if (!isMember) {
            throw new CustomException(ErrorCode.POST_ACCESS_DENIED);
        }
    }
}
```

### 5.3 멱등성 처리

```java
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    @Override
    public boolean isDuplicate(String key) {
        return redisTemplate.hasKey(KEY_PREFIX + key);
    }

    @Override
    public <T> T getExistingResponse(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + key);
        return objectMapper.readValue(json, type);
    }

    @Override
    public <T> void saveResponse(String key, T response) {
        String json = objectMapper.writeValueAsString(response);
        redisTemplate.opsForValue().set(KEY_PREFIX + key, json, TTL);
    }
}
```

### 5.4 Kafka Producer

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void send(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka 메시지 발행 실패: topic={}, key={}", topic, key, ex);
                    }
                });
        } catch (JsonProcessingException e) {
            log.error("Kafka 이벤트 직렬화 실패", e);
        }
    }
}
```

### 5.5 파일 업로드 (S3)

```java
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final AmazonS3 s3Client;
    private final FileRepository fileRepository;

    @Value("${aws.s3.bucket}")
    private String bucket;

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;   // 10MB
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;    // 20MB
    private static final int MAX_FILES_PER_POST = 5;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
        "application/pdf", "text/plain"
    );

    @Override
    public FileResponse upload(MultipartFile file, TargetType targetType, Long userId) {
        validateFile(file);

        String storedName = UUID.randomUUID() + getExtension(file.getOriginalFilename());
        String s3Path = targetType.name().toLowerCase() + "/" + storedName;

        // S3 업로드
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        s3Client.putObject(bucket, s3Path, file.getInputStream(), metadata);

        // DB 저장
        File entity = File.builder()
            .userId(userId)
            .targetType(targetType)
            .originalName(file.getOriginalFilename())
            .storedName(storedName)
            .s3Path(s3Path)
            .size(file.getSize())
            .mimeType(file.getContentType())
            .build();

        return fileMapper.toResponse(fileRepository.save(entity));
    }
}
```

### 5.6 검색 구현

```java
// PostRepository
@Query("""
    SELECT p FROM Post p
    WHERE p.boardId = :boardId
      AND p.deletedAt IS NULL
      AND (:keyword IS NULL OR (
        (:searchType = 'TITLE' AND p.title LIKE %:keyword%) OR
        (:searchType = 'CONTENT' AND p.content LIKE %:keyword%) OR
        (:searchType = 'TITLE_CONTENT' AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)) OR
        (:searchType = 'NICKNAME' AND p.user.nickname LIKE %:keyword%)
      ))
      AND (:cursor IS NULL OR p.id < :cursor)
    ORDER BY p.id DESC
    """)
List<Post> findByBoardWithSearch(
    @Param("boardId") Long boardId,
    @Param("keyword") String keyword,
    @Param("searchType") String searchType,
    @Param("cursor") Long cursor,
    Pageable pageable
);
```

---

## 6. AI Service

> **TBD**: LLM 모델 미결정
> - LlmClient 구현은 선택된 모델의 API 스펙에 따라 변경됨
> - 후보: OpenAI GPT, Anthropic Claude, Google Gemini, 국내 LLM 등
> - 모델 결정 후 LlmClientConfig, PromptTemplates 구체화 필요

### 6.1 패키지 구조

```
com.lockerroom.aiservice/
├── configuration/
│   ├── KafkaConsumerConfig.java
│   └── LlmClientConfig.java
├── controller/
│   └── AiController.java
├── service/
│   ├── AiNewsService.java
│   ├── AiCommentService.java
│   ├── impl/
│   │   ├── AiNewsServiceImpl.java
│   │   └── AiCommentServiceImpl.java
├── kafka/
│   └── QnaPostConsumer.java
├── client/
│   ├── LlmClient.java                # LLM API 호출
│   └── ResourceServiceClient.java     # resource-service REST 호출
├── dto/ ...
├── exceptions/ ...
└── utils/
    └── PromptTemplates.java
```

### 6.2 Kafka Consumer (Q&A 자동 답변)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class QnaPostConsumer {

    private final AiCommentService aiCommentService;

    @KafkaListener(
        topics = "qna-post.created",
        groupId = "ai-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            QnaPostCreatedEvent event = objectMapper.readValue(
                record.value(), QnaPostCreatedEvent.class);
            aiCommentService.generateAndPost(event);
        } catch (Exception e) {
            log.error("Q&A 자동 답변 처리 실패: {}", record.value(), e);
            throw e; // DLQ로 이동
        }
    }
}
```

### 6.3 뉴스 자동 생성 (스케줄러)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class NewsScheduler {

    private final AiNewsService aiNewsService;
    private final ResourceServiceClient resourceClient;

    @Scheduled(cron = "0 0 7 * * *")  // 매일 오전 7시
    public void generateDailyNews() {
        List<Team> activeTeams = resourceClient.getActiveTeams();
        for (Team team : activeTeams) {
            try {
                aiNewsService.generateAndPost(team);
            } catch (Exception e) {
                log.error("팀 뉴스 생성 실패: teamId={}", team.getId(), e);
            }
        }
    }
}
```

---

## 7. Notification Service

### 7.1 패키지 구조

```
com.lockerroom.notificationservice/
├── configuration/
│   ├── KafkaConsumerConfig.java
│   ├── MailConfig.java
│   └── SmsConfig.java
├── kafka/
│   ├── NotificationConsumer.java
│   └── EmailConsumer.java
├── service/
│   ├── EmailService.java
│   ├── SmsService.java
│   ├── InAppNotificationService.java
│   ├── impl/
│   │   ├── EmailServiceImpl.java
│   │   ├── SmsServiceImpl.java
│   │   └── InAppNotificationServiceImpl.java
├── client/
│   └── ResourceServiceClient.java
├── dto/ ...
└── exceptions/ ...
```

### 7.2 Kafka Consumer

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final InAppNotificationService notificationService;

    @KafkaListener(
        topics = {"notification.comment", "notification.reply",
                  "notification.inquiry-replied", "notification.report-processed"},
        groupId = "notification-consumer-group"
    )
    public void consume(ConsumerRecord<String, String> record) {
        NotificationEvent event = objectMapper.readValue(
            record.value(), NotificationEvent.class);
        notificationService.createNotification(event);
    }
}

@Component
@RequiredArgsConstructor
public class EmailConsumer {

    private final EmailService emailService;

    @KafkaListener(
        topics = "email.password-reset",
        groupId = "notification-consumer-group"
    )
    public void consume(ConsumerRecord<String, String> record) {
        EmailEvent event = objectMapper.readValue(
            record.value(), EmailEvent.class);
        emailService.sendPasswordResetEmail(event);
    }
}
```

### 7.3 DLQ 재시도 설정

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template) {
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(template,
            (record, ex) -> new TopicPartition(record.topic() + ".dlq", 0));

    BackOff backOff = new FixedBackOff(5000L, 3L);  // 5초 간격, 3회 재시도
    return new DefaultErrorHandler(recoverer, backOff);
}
```

---

## 8. 설정 전략

### 8.1 application.yml 구조

```yaml
# application.yml (공통)
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

---
# application-local.yml
spring:
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: jdbc:mariadb://localhost:3306/lockerroom
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  data:
    redis:
      host: localhost
      port: 6379

  kafka:
    bootstrap-servers: localhost:9092

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/locker-room}

keycloak:
  admin:
    server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8180}
    realm: ${KEYCLOAK_REALM:locker-room}
    client-id: ${KEYCLOAK_ADMIN_CLIENT_ID:admin-cli}
    client-secret: ${KEYCLOAK_ADMIN_CLIENT_SECRET}

aws:
  s3:
    bucket: ${S3_BUCKET}
    region: ${AWS_REGION}

app:
  cors:
    allowed-origins: http://localhost:3000
```

### 8.2 환경별 설정
| 환경 | Profile | 설명 |
|------|---------|------|
| 로컬 | local | 로컬 개발 환경 |
| 개발 | dev | 개발 서버 |
| 스테이징 | staging | QA/테스트 |
| 운영 | prod | 프로덕션 |

---

## 9. 테스트 전략

### 9.1 테스트 레이어

| 레이어 | 대상 | 도구 | 비고 |
|--------|------|------|------|
| Unit Test | Service, Util | JUnit 5 + Mockito | Mocking 기반 |
| Integration Test | Repository | @DataJpaTest + H2 | DB 연동 |
| Controller Test | Controller | @WebMvcTest + MockMvc | 요청/응답 검증 |
| E2E Test | 전체 API 흐름 | @SpringBootTest + TestRestTemplate | 실제 환경 유사 |

### 9.2 테스트 네이밍 규칙
```
{메서드명}_{시나리오}_{기대결과}
```
예시:
```java
@Test
void createPost_validRequest_returnsCreated() { ... }

@Test
void createPost_invalidBoardId_throwsNotFoundException() { ... }

@Test
void toggleLike_alreadyLiked_removesLike() { ... }
```

### 9.3 테스트 커버리지 목표
| 레이어 | 목표 |
|--------|------|
| Service | 80% 이상 |
| Controller | 70% 이상 |
| Repository | 60% 이상 |

---

## 10. 로깅 전략

### 10.1 로그 레벨
| 환경 | 기본 레벨 | 패키지별 |
|------|-----------|----------|
| local | DEBUG | - |
| dev | DEBUG | - |
| staging | INFO | `com.lockerroom`: DEBUG |
| prod | INFO | `com.lockerroom`: INFO |

### 10.2 로그 포맷
```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - [%X{traceId}] %msg%n
```

### 10.3 로그 분류
| 유형 | 위치 | 내용 |
|------|------|------|
| 요청/응답 | Filter/Interceptor | Method, URI, 상태 코드, 응답 시간 |
| 비즈니스 | Service | 주요 비즈니스 이벤트 (게시글 작성, 좋아요 등) |
| 에러 | ExceptionHandler | 스택 트레이스, 요청 정보 |
| Kafka | Producer/Consumer | 토픽, 파티션, 오프셋, 처리 결과 |
| 외부 연동 | Client | 요청/응답, 응답 시간 |

### 10.4 로그 저장
- 로컬: 콘솔 + 파일 (logs/ 디렉토리)
- 운영: AWS S3 (일별 로테이션, 90일 보관)

---

## 11. 빌드 및 배포

### 11.1 Maven 멀티 모듈 (선택적)

```
locker-room-backend/
├── pom.xml                 # Parent POM
├── gateway/
│   └── pom.xml
├── auth-service/
│   └── pom.xml
├── resource-service/
│   └── pom.xml
├── ai-service/
│   └── pom.xml
├── notification-service/
│   └── pom.xml
└── common/                 # 공통 모듈
    └── pom.xml
```

### 11.2 Docker 빌드

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 11.3 CI/CD 파이프라인 (Jenkins)

```
1. 코드 Push → GitHub Webhook → Jenkins 트리거
2. Build: mvn clean package -DskipTests
3. Test: mvn test
4. Docker Build: docker build -t lockerroom/{service}:{version}
5. Docker Push: AWS ECR에 이미지 Push
6. Deploy: AWS ECS 태스크 정의 업데이트 → EC2 Rolling Deployment
```

---

## 12. API 문서화

### 12.1 Swagger (SpringDoc OpenAPI)

```java
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Locker Room API")
                .version("v1.0")
                .description("스포츠 팬 커뮤니티 API"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer"))
            .components(new Components()
                .addSecuritySchemes("Bearer",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

- 접근 URL: `http://localhost:{port}/swagger-ui.html`
- Postman Collection은 Claude POSTMAN MCP를 이용하여 업로드

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-15 | - | 초안 작성 |
| 1.1 | 2026-02-15 | - | 패키지명 규칙 변경 (com.lockerroom.resourceservice), 빌드 도구 Maven 변경, MariaDB Connector/J 명시 |
| 1.2 | 2026-02-16 | - | 인증 서버 Keycloak 확정. Auth Service JWT 자체 구현 → Keycloak 연동으로 변경, application.yml Keycloak 설정 추가 |
