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

```
src/main/java/com/lockerroom/resourceservice/
├── aop/               # IdempotencyAspect, @Idempotent
├── configuration/     # Security, JPA, Redis, S3, Swagger, CORS, MessageSource
├── controller/        # REST API 컨트롤러
├── dto/
│   ├── request/       # 요청 DTO (Jakarta Validation)
│   └── response/      # 응답 DTO (Java Record)
├── exceptions/        # CustomException, ErrorCode, GlobalExceptionHandler
├── kafka/             # KafkaProducerService, Event 클래스
├── mapper/            # MapStruct Mapper 인터페이스
├── model/
│   ├── entity/        # JPA Entity (sport별 분리: Football*/Baseball*)
│   └── enums/         # 도메인 Enum
├── repository/        # Spring Data JPA Repository
├── security/          # KeycloakRoleConverter, @CurrentUserId Resolver
├── service/           # Service 인터페이스 + impl/
└── utils/             # Constants, MessageUtils
```

> 일부 도메인(Post/Board/Team/League)은 sport별 테이블로 분리되어 있어 `Football*`, `Baseball*` 접두사 엔티티가 함께 존재합니다.

## API Endpoints

| Domain | Endpoints | Base Path |
|--------|:---------:|-----------|
| Info | 2 | `/api` |
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
| `/api/v1/admin/**`, `/actuator/**` (health/info 제외) | `ROLE_ADMIN` |
| `/swagger-ui/**`, `/api-docs/**`, `/api/name`, `/api/version`, `/actuator/health`, `/actuator/info` | 인증 불필요 |
| 그 외 | 인증 필요 |

## Idempotency

쓰기 성격의 일부 엔드포인트는 `@Idempotent` AOP로 멱등성을 강제합니다. 클라이언트는 `Idempotency-Key` 헤더(최대 128자)를 보내야 하며, 동일 키 재전송 시 캐싱된 응답을 그대로 반환합니다. Redis가 미가용이면 인메모리 fallback을 사용합니다.
