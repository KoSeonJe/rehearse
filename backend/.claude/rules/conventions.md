# Backend Conventions

도메인 기반 백엔드 컨벤션. 커밋 / 보안 / 주석 / 플랜은 루트 `.claude/rules/`.

## 패키지 구조

```
com.rehearse.api/
├── domain/{feat}/
│   ├── controller/
│   ├── service/                # 애플리케이션 서비스 (트랜잭션, 도메인 호출 조립)
│   ├── repository/             # JPA repo + cache 래퍼
│   ├── dto/{request,response}/
│   ├── exception/              # 도메인 예외
│   ├── event/                  # 발행 이벤트 + 외부 이벤트 listener
│   └── models/
│       ├── entity/             # 테이블 엔티티 + sub-aggregate 동거
│       ├── vo/
│       ├── service/            # 도메인 서비스 + port 인터페이스
│       └── enums/
├── global/
│   ├── auth/                   # OAuth callback + JWT + Security 필터 통합
│   ├── config/                 # 도메인 무관 Spring Configuration
│   ├── exception/              # 전역 예외 (ResourceNotFound 등) + GlobalExceptionHandler
│   ├── common/                 # ApiResponse, ErrorResponse
│   └── util/
└── infra/{ext}/
    ├── adapter/                # port 구현
    ├── config/                 # 외부 SDK 설정
    └── client/                 # SDK 클라이언트
```

### 도메인 패키지 룰

- `service/` = 애플리케이션 서비스 (트랜잭션 경계, 조립). `models/service/` = 도메인 서비스 + port 인터페이스.
- Sub-aggregate 별도 패키지 X → 상위 `models/entity/` 동거 (예: `feedback/models/entity/Rubric.java`).
- DTO 매핑은 dto 안 정의 (mapper 패키지 폐지): DTO→Entity = `toXxEntity()`, DTO→VO = `toXx()`, Entity→DTO = `from(단수)` / `of(복수)`.
- `policy/` X → `models/{entity|vo|service}` 적절 위치.
- `cache/` X → `repository/` 하위.
- `vo/` 도메인 직속 X → `models/vo/`.
- 중첩 `domain/` X → `models/entity/`.
- 도메인 내부 `config/` X → `global/config/` (도메인 무관) 또는 `infra/{ext}/config/` (SDK).
- Cross-domain: app service → 같은 aggregate **상위 → 하위** 만 (하위 → 상위 금지). Repository 직접 접근 허용. 도메인 간 이벤트 통신 가능.

### Port / Adapter

- Port 위치 = 사용 도메인 `models/service/` 인터페이스. 책임 단위 분리 (예: `QuestionGenerator`, `FeedbackCoach`). 거대 단일 인터페이스 X.
- Adapter 구현 = `infra/{ext}/adapter/`.
- infra 내 controller / service / repository 절대 금지 — adapter / config / client 만.

### Event

- 자기 상태 알림 전용.
- 페이로드 = 식별자 + 행위 + 속성 + 이벤트시간 (그 외 금지).
- 발행 클래스 = 발행 도메인 `event/`. Listener = 수신 도메인 `event/` 안 (외부 이벤트 listener 도 동거).
- 정합성: `@TransactionalEventListener(phase = AFTER_COMMIT)` 강제.

### 테스트 패키지 미러링

- `src/test/java/com/rehearse/api/domain/{feat}/{controller|service|repository|...}/` — 프로덕션 동일 구조.
- Support 클래스: `src/test/java/com/rehearse/api/support/` (testing.md).

## 네이밍

### 기본

| 대상 | 룰 | 예시 |
|------|----|------|
| 클래스 | PascalCase | `InterviewController` |
| 메서드 / 변수 | camelCase | `createInterview()` |
| 상수 / Enum 값 | UPPER_SNAKE_CASE | `MAX_QUESTIONS` |
| 패키지 | 소문자, 단어구분 없음 | `interview`, `questionset` |
| DB 테이블 | snake_case 단수 | `interview`, `interview_question` |
| DB 컬럼 | snake_case | `created_at`, `interview_type` |
| URL 경로 | kebab-case | `/interview-questions` |

### 클래스 접미사

| 종류 | 접미사 | 예시 |
|------|-------|------|
| Controller | `*Controller` | `InterviewController` |
| App Service | `*Service` | `InterviewService` |
| Domain Service | **책임 명사** (접미사 X) | `QuestionGenerator`, `FeedbackCoach` |
| Repository | `*Repository` | `InterviewRepository` |
| Configuration | `*Config` | `AsyncConfig` |
| Exception | `*Exception` | `InterviewNotFoundException` |
| Event | `*Event` | `InterviewCompletedEvent` |
| Listener | `*EventListener` | `FeedbackEventListener` |
| Adapter | `*Adapter` | `OpenAiAdapter` |
| Persister | `*Persister` | `ResumeSkeletonPersister` |
| Runtime cache | `*RuntimeCache` | `InterviewRuntimeStateCache` |

### Entity / DTO / 필드

- Entity: 도메인 명사. VO record 충돌 시만 `*Entity` 접미사.
- DTO: `*Request` / `*Response`.
- ID: PK = `id`, FK = `<도메인>Id`. Boolean: `is*` / `has*` / `can*`. 시간: `*At`. Collection: 복수형.
- ErrorCode: `{DOMAIN}_{3자리}` (`INTERVIEW_001`, `AI_001`).

### Port 인터페이스

- 책임 단위 명사. 거대 단일 인터페이스 (`AiClient` 류) X. 예: `QuestionGenerator`, `FollowUpQuestionGenerator`, `FeedbackCoach`, `Stt`.

### 테스트

- 메서드: `<예상결과>_when_<조건>` 또는 `should_<예상결과>_when_<조건>`.
- `@DisplayName` 한국어 + `@Nested` 시나리오 그룹 (testing.md).

## 계층 책임

| 계층 | 허용 | 금지 |
|------|------|------|
| Controller | HTTP 처리, `@Valid`, App Service 호출 | 비즈로직, Repository 직접 호출 |
| App Service | 트랜잭션 경계, Domain Service / Repository 조립 | HTTP 객체 의존 (`HttpServletRequest`, `ResponseEntity`), 깊은 비즈로직 |
| Domain Service | 도메인 로직, port 호출 | HTTP / DTO 의존 |
| Repository | 데이터 접근 | 비즈로직 |

- Entity 직접 반환 금지 — 모든 응답 Response DTO 변환.

## 메서드 단일 책임 (SRP)

- **한 메서드 = 한 책임**. 다중 책임 결합 금지.
- **`catch (A | B)` 다중 예외 처리 금지** — 서로 다른 의미의 예외 (예: 파싱 실패 vs 검증 실패) 한 catch 절에서 묶지 말 것. 예외 의미가 같은 경우 (`UnknownHostException | ConnectException` = 네트워크 도달 실패) 만 허용.
- **이름의 `And`/`Or` 의심**. `parseAndValidate`, `saveOrUpdate` = 책임 둘. 메서드 분리 → 호출부에서 조립. 단순 조립자 (`fetchUser` = `findById` + `mapToDto`) 는 허용.
- **신호 (Smell)**: 메서드 본문 내 두 if 분기가 각각 다른 사이드이펙트 / 다른 도메인 호출 → 분리 후보.

```java
// ❌ 다중 책임 (catch or)
try {
    return objectMapper.readValue(json, clazz);
} catch (JsonProcessingException | IllegalArgumentException e) {
    return retryWithSchemaHint(...);  // 파싱 실패 + 검증 실패 한 흐름 처리
}

// ✅ 책임 분리
ParsedXxx parsed = parseOrRetry(json, clazz);          // JSON 파싱 + parse retry
return validateOrRetry(parsed, clazz, json);           // 검증 + validation retry
```

- **예외 정책 동일 시**: 두 책임 catch 흐름이 우연히 동작 같다는 이유로 묶지 말 것. 책임이 다르면 메서드 분리 + 정책 변경 시 독립 진화 가능.

## DTO 패턴

- **Request**: `@Getter` + `@NoArgsConstructor` + 필드 Bean Validation 어노테이션.
  ```java
  @Getter @NoArgsConstructor
  public class CreateInterviewRequest {
      @NotNull(message = "직무를 선택해주세요.")
      private Position position;
  }
  ```
- **Response**: `@Getter` + `@Builder` + 정적 팩토리 (`from` 단수 / `of` 복수). 매핑 메서드는 dto 안 정의.

## 에러 처리

- `BusinessException` + `ErrorCode` 인터페이스. 코드 = `{DOMAIN}_{3자리}`.
- 도메인 무관 (`ResourceNotFound`, `InvalidArgument` 등) → `global/exception/`. 도메인 관련 → `domain/{feat}/exception/`.
- 통일 에러 응답 = `GlobalExceptionHandler` (`global/exception/`).
- 응답: 성공 = `ApiResponse.ok(data)`. 실패 = `GlobalExceptionHandler` → `ErrorResponse`.

## 트랜잭션

- 읽기 = `@Transactional(readOnly = true)` 기본. 쓰기 = `@Transactional`.
- 위치 = App Service. Domain Service 도 **repository 사용 시** `@Transactional` 적용.
- Controller / Repository 에 `@Transactional` 금지.

## Flyway 마이그레이션

- **DDL 전용**. `CREATE/ALTER/DROP TABLE`, `ADD/DROP INDEX`, `ADD/DROP COLUMN`, `ADD/DROP CONSTRAINT` 등.
- **DML 금지**. `INSERT/UPDATE/DELETE` 데이터 정리·시드·백필 X. 데이터 cleanup → 운영 SQL 분리. 시드 → `data.sql` (dev) 또는 별도 스크립트.
- Idempotent 작성. DDL 이 기존 데이터 제약 위반 가능 (예: `ADD UNIQUE INDEX`) → 사전 운영 SQL 정리 후 DDL.
- 본 룰은 **현 시점부터 적용**. 과거 위반 V 파일은 그대로 유지 (재작성 금지).

## LLM strict JSON schema 정의 위치

OpenAI `response_format=json_schema` (strict=true) 사용 site 의 schema 는 반드시 `infra/ai/schema/{DtoName}Schema.java` 에 정의한다.

- DTO 1:1 매핑. 클래스명 = `{DtoName}Schema`.
- `public static final String NAME` + `public static Map<String, Object> build()` (또는 `build(args)`) + `public static JsonSchemaSpec spec()` (또는 `spec(args)`).
- Adapter / Service / PromptBuilder 어디서든 동일 schema 클래스 호출. inline 정의 금지.
- `additionalProperties: false` 강제.

## Lombok

| 허용 | 금지 |
|------|------|
| `@Getter` | `@Data` |
| `@NoArgsConstructor` | `@AllArgsConstructor` |
| `@Builder` | `@Setter` |
| `@RequiredArgsConstructor` | `@ToString` (entity) |
| `@Slf4j` | `@EqualsAndHashCode` (entity) |

## 입력 검증

- Controller `@Valid` = 단순 형식만 (`@NotNull`, `@NotBlank`, `@Min`, `@Max`, `@Size`, `@Pattern`).
- 도메인 룰 (값 범위, 상태 전이, 비즈니스 제약) = Service / Domain Service.

## Logging

- SLF4J `@Slf4j` 만 사용. `System.out.println`, `printStackTrace()` 금지.
- 메시지 **한국어**. 도메인 ID 컨텍스트 포함 (userId, interviewId 등).
- 레벨: `INFO` 정상 흐름 / `WARN` 복구 가능 비정상 / `ERROR` 실패 + stack trace / `DEBUG` 개발용 (운영 X).
- 포맷: placeholder + key=value. 문자열 concat 금지.
- 예외 로깅: throwable 인자로 + **재던짐** (또는 BusinessException 래핑). 로그만 찍고 삼키기 X.
  ```java
  } catch (Exception e) {
      log.error("AI 호출 실패 sessionId={}", sessionId, e);
      throw new BusinessException(AI_001);
  }
  ```
- **민감정보 금지**: password, token, JWT, API 키, 카드번호, 주민번호 (마스킹 또는 제외).
