# Locker Room Resource Service

Locker Room 스포츠 커뮤니티 플랫폼의 리소스 관리 마이크로서비스입니다.

## Tech Stack

- **Java 17** / **Spring Boot 4.0.2**
- **Spring Data JPA** + Hibernate 7 / MariaDB
- **Spring Security 7** (Gateway 헤더 기반 인증)
- **MapStruct** (Entity-DTO 변환)
- **Apache Kafka** (이벤트 발행)
- **Redis** (멱등성 처리)
- **AWS S3** (파일 업로드)

## Project Structure

```
src/main/java/com/lockerroom/resourceservice/
├── configuration/     # Security, JPA, Redis, S3, Swagger, CORS 설정
├── controller/        # REST API 컨트롤러 (11개, 46 엔드포인트)
├── dto/
│   ├── request/       # 요청 DTO (Jakarta Validation)
│   └── response/      # 응답 DTO (Java Record)
├── exceptions/        # CustomException, ErrorCode, GlobalExceptionHandler
├── kafka/             # KafkaProducerService, Event 클래스
├── mapper/            # MapStruct Mapper 인터페이스 (8개)
├── model/
│   ├── entity/        # JPA Entity (17개)
│   └── enums/         # Enum (12개)
├── repository/        # Spring Data JPA Repository (17개)
├── security/          # GatewayAuthenticationFilter
├── service/           # Service 인터페이스 + 구현체 (12개)
└── utils/             # Constants, MessageUtils
```

## API Endpoints

| Domain | Endpoints | Base Path |
|--------|:---------:|-----------|
| Users | 6 | `/api/v1/users` |
| Sports | 2 | `/api/v1/sports` |
| Boards | 2 | `/api/v1/boards` |
| Posts | 6 | `/api/v1/posts` |
| Comments | 5 | `/api/v1/posts/{postId}/comments`, `/api/v1/comments` |
| Notices | 2 | `/api/v1/notices` |
| Inquiries | 3 | `/api/v1/inquiries` |
| Requests | 3 | `/api/v1/requests` |
| Notifications | 4 | `/api/v1/notifications` |
| Files | 2 | `/api/v1/files` |
| Admin | 11 | `/api/v1/admin` |
| **Total** | **46** | |

## Getting Started

### Prerequisites

- Java 17+
- MariaDB
- Redis (optional, in-memory fallback available)
- Kafka (optional, graceful skip when unavailable)

### Run (Local)

```bash
./mvnw spring-boot:run
```

Local 프로파일(`application-local.yaml`)에서는 H2 인메모리 DB를 사용하며, Redis/Kafka/S3 없이 기동 가능합니다.

- Application: `http://localhost:8082`
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- H2 Console: `http://localhost:8082/h2-console`

### Run Tests

```bash
./mvnw test
```

178개 테스트 (Service 74 + Controller 39 + Repository 64 + 1 skipped)

## Authentication

API Gateway에서 JWT 검증 후 전달하는 헤더를 사용합니다:

| Header | Description |
|--------|-------------|
| `X-User-Id` | 사용자 ID |
| `X-User-Role` | 사용자 역할 (`USER`, `ADMIN`) |

`/api/v1/admin/**` 엔드포인트는 `ADMIN` 역할만 접근 가능합니다.
