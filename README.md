# Locker Room Resource Service

Locker Room 스포츠 커뮤니티 플랫폼의 리소스 관리 마이크로서비스입니다.

## Tech Stack

- **Java 17** / **Spring Boot 4.0.2**
- **Spring Data JPA** + Hibernate 7 / MariaDB
- **Spring Security** + **OAuth2 Resource Server** (Keycloak JWT)
- **MapStruct** (Entity ↔ DTO 변환)
- **Apache Kafka** (도메인 이벤트 발행)
- **Redis** (멱등성 처리, 미가용 시 인메모리 fallback)
- **AWS S3** (파일 업로드)
- **springdoc-openapi** (Swagger UI)

## Project Structure

**Package-by-Feature** 구조. 도메인 단위로 controller/service/repository/model/mapper/dto를 한 폴더에 묶어 응집도를 높이고 향후 모듈러 모놀리스/DDD 진화를 쉽게 합니다.

```
src/main/java/com/lockerroom/resourceservice/
├── common/                  # 전 도메인 공유 (응답 래퍼, 페이징, BaseEntity, Role)
├── infrastructure/          # 기술 인프라 (도메인 비특정)
│   ├── aop/                 #   @Idempotent, @RateLimit
│   ├── configuration/       #   Security, JPA, Redis, S3, Swagger, MessageSource
│   ├── controller/          #   InfoController (운영 정보)
│   ├── exceptions/          #   CustomException, ErrorCode, GlobalExceptionHandler
│   ├── kafka/               #   KafkaProducerService
│   ├── security/            #   KeycloakRoleConverter, @CurrentUserId
│   └── utils/               #   Constants, MessageUtils
│
├── user/                    # 사용자 (User, UserTeam, UserSuspension, UserWithdrawal)
├── post/                    # 게시글 + 좋아요 + 신고 (sport별 분리)
├── comment/                 # 댓글 (depth 1까지)
├── board/                   # 게시판 (sport별 분리: Football/Baseball)
├── sport/                   # 스포츠/리그/팀/태그/국가
├── file/                    # 파일 업로드 (S3 + TargetType 추상화)
├── inquiry/                 # 1:1 문의 + 답변
├── request/                 # 신규 팀/리그 등록 요청
├── notice/                  # 공지사항
├── notification/            # 알림 (Kafka 이벤트 수신)
├── admin/                   # 관리자 cross-cutting (사용자/신고/공지/문의/요청)
└── ResourceServiceApplication.java
```

각 도메인 폴더는 다음 표준 구조를 따릅니다:

```
{domain}/
├── controller/              # REST API 엔드포인트
├── service/{,impl/}         # 인터페이스 + 구현체
├── repository/              # Spring Data JPA Repository
├── model/{entity,enums}/    # JPA Entity, 도메인 Enum
├── mapper/                  # MapStruct (단일 도메인 한정)
├── event/                   # Kafka 이벤트 (해당 도메인이 발행하는 경우)
└── dto/{request,response}/  # 요청/응답 DTO
```

> **Sport-specific 분리**: Post/Board/Team/League는 종목별 테이블로 분리되어 있어 `Football*`, `Baseball*` 접두사 엔티티가 함께 존재합니다.

## API Endpoints

| Domain | Endpoints | Base Path |
|--------|:---------:|-----------|
| Info | 2 | `/api/v1/info` |
| Users | 6 | `/api/v1/users` |
| Sports | 1 | `/api/v1/sports` |
| Boards | 2 | `/api/v1/boards` |
| Posts | 7 | `/api/v1/posts` |
| Comments | 5 | `/api/v1/posts/{postId}/comments`, `/api/v1/comments` |
| Notices | 2 | `/api/v1/notices` |
| Inquiries | 3 | `/api/v1/inquiries` |
| Requests | 3 | `/api/v1/requests` |
| Notifications | 4 | `/api/v1/notifications` |
| Files | 2 | `/api/v1/files` |
| Admin | 14 | `/api/v1/admin` |
| **Total** | **51** | |

전 엔드포인트는 한국어 `@Tag` / `@Operation` / `@ApiResponse` 어노테이션과 DTO 필드 단위 `@Schema` 메타데이터를 가집니다. Swagger UI에서 의미·예시·필수 여부·에러 코드를 모두 확인할 수 있습니다.

## Getting Started

### Prerequisites

- Java 17+
- MariaDB (필수 — `application.yaml`의 datasource 기본값)
- Redis (선택 — 미가용 시 멱등성은 인메모리 fallback)
- Kafka (선택 — `local` 프로파일에서는 자동 설정 제외)
- Keycloak Issuer (`https://auth.greatpark.co.kr/realms/locker-room`) 또는 동등한 OAuth2 IdP

### 환경 변수

| Variable | Default | 설명 |
|----------|---------|------|
| `DB_URL` | `jdbc:mariadb://localhost:3306/locker_room?createDatabaseIfNotExist=true` | DataSource URL |
| `DB_USERNAME` | `root` | DB 사용자 |
| `DB_PASSWORD` | (빈문자) | DB 비밀번호 |
| `SPRING_PROFILES_ACTIVE` | `local` | 활성 프로파일 |

### Run (Local)

```bash
./mvnw spring-boot:run
```

`local` 프로파일(`application-local.yaml`)은 다음을 적용합니다:
- `ddl-auto: update` (스키마 자동 업데이트)
- `show-sql: true`
- Redis / Kafka 자동 설정 제외

엔드포인트:
- Application: `http://localhost:8082`
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/api-docs`
- 운영 정보: `http://localhost:8082/api/v1/info/{name,version}`

### Run Tests

```bash
./mvnw test
```

총 240개 테스트 (1개 skipped) — Service / Controller / Repository / Security 계층 커버.

### Build / Docker

```bash
./mvnw clean package -DskipTests
docker build -t locker-room/resource-service .
```

## Authentication & Authorization

OAuth2 Resource Server를 사용해 **Keycloak JWT를 직접 검증**합니다 (`spring-boot-starter-oauth2-resource-server`).

| 항목 | 설명 |
|------|------|
| Issuer | `application.yaml`의 `spring.security.oauth2.resourceserver.jwt.issuer-uri` |
| Subject claim | Keycloak `sub` (= `keycloakId`) |
| Role claim | `realm_access.roles` → `KeycloakRoleConverter`가 `ROLE_*` GrantedAuthority로 매핑 |
| User 식별 | `@CurrentUserId Long userId` — 컨트롤러 파라미터에서 JWT subject를 internal `User.id`로 변환 |

요청 흐름:
1. 클라이언트가 `Authorization: Bearer <JWT>` 헤더로 호출
2. `OAuth2ResourceServer`가 issuer에서 JWKS 받아 서명 검증
3. `KeycloakRoleConverter`가 realm role을 Spring `GrantedAuthority`로 변환
4. `CurrentUserIdArgumentResolver`가 JWT `sub` → `users.keycloak_id` 조회 → `User.id` 주입

엔드포인트 권한:

| 패턴 | 권한 |
|------|------|
| `GET /api/v1/notices/**`, `/api/v1/sports/**`, `/api/v1/boards/**` | 인증 불필요 |
| `GET /api/v1/posts/{postId}`, `/api/v1/posts/popular`, `/api/v1/posts/{postId}/comments` | 인증 불필요 |
| `GET /api/v1/info/**` | 인증 불필요 |
| `/swagger-ui/**`, `/api-docs/**`, `/actuator/health`, `/actuator/info` | 인증 불필요 |
| `/api/v1/admin/**`, `/actuator/**` (health/info 제외) | `ROLE_ADMIN` |
| 그 외 | 인증 필요 |

Swagger 측면에서는 `SwaggerConfig`가 모든 엔드포인트에 `bearerAuth` 보안 스키마를 기본 적용하며, 익명 허용 엔드포인트는 컨트롤러에 `@SecurityRequirements`(빈 배열)로 자물쇠를 제거합니다.

## Idempotency

쓰기 성격의 일부 엔드포인트는 `@Idempotent` AOP로 멱등성을 강제합니다. 클라이언트는 `Idempotency-Key` 헤더(최대 128자)를 보내야 하며, 동일 키 재전송 시 캐싱된 응답을 그대로 반환합니다. Redis가 미가용이면 인메모리 fallback을 사용합니다.

적용 엔드포인트 (예): `POST /api/v1/posts`, `POST /api/v1/posts/{postId}/like`, `POST /api/v1/posts/{postId}/report`, `POST /api/v1/posts/{postId}/comments`, `POST /api/v1/comments/{commentId}/replies`, `POST /api/v1/inquiries`, `POST /api/v1/requests`.
