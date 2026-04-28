# Backend 컨벤션

> 유지보수와 협업을 위한 백엔드 코딩 규칙.
> 공통 규칙(브랜치, 커밋, PR)은 `docs/conventions.md` 참조.
> 실전 구현 가이드는 `CODING_GUIDE.md` 참조.

---

## 패키지 구조 (도메인 기반)

```
com.rehearse.api/
├── domain/{feature}/       # 도메인별 분리
│   ├── controller/         # @RestController (HTTP 요청/응답)
│   ├── service/            # 비즈니스 로직, @Transactional
│   ├── repository/         # Spring Data JPA 인터페이스
│   ├── entity/             # JPA 엔티티, Enum, record VO
│   ├── dto/                # 요청/응답 DTO
│   ├── exception/          # 도메인별 ErrorCode enum
│   ├── event/              # [선택] 도메인 이벤트 + Listener (service/event/ 중첩 금지)
│   ├── mapper/             # [선택] 변환 로직 5줄 이상 + 테스트 필요 시
│   └── {sub-aggregate}/    # [선택] 자체 entity/repository 가진 하위 집합 (예: rubric/)
├── global/                 # 전역 공통
│   ├── config/             # Spring Configuration (도메인 무관 공통 config 모두 여기)
│   ├── exception/          # 글로벌 예외 핸들러
│   ├── common/             # ApiResponse, ErrorResponse
│   └── util/               # 공통 유틸리티
└── infra/                  # 외부 서비스 연동
    ├── ai/                 # AI 클라이언트
    ├── aws/                # AWS 연동 (AwsConfig는 여기 유지)
    └── google/             # Google 연동 (GoogleTtsConfig는 여기 유지)
```

### 도메인 내부 패키지 표준

**필수 6개**: `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `exception/`

**선택 3개** (필요 시만 생성):
- `event/` — 도메인 이벤트 클래스 + `@TransactionalEventListener`. `service/event/` 중첩 금지
- `mapper/` — 변환 로직 5줄 이상이고 단독 테스트가 필요한 경우. 단순 `Response.from()` 팩토리는 DTO 안에 유지
- `{sub-aggregate}/` — 자체 `entity/`, `repository/`를 보유한 독립 하위 집합 (예: `feedback/rubric/`)

**금지 패키지** (발견 시 표준 폴더로 이전):
- `policy/` → `service/` 안 클래스로 (Strategy 패턴도 service/ 안)
- `vo/` → `entity/` 안으로 통합 (record VO 포함)
- `cache/` → `service/` 안 `*RuntimeCache` 클래스로
- `domain/` (중첩) → `entity/` 로 (예: `resume/domain/` 금지)
- `intent/`, `turn/` 등 임의 중간 폴더 → `service/` 안 단일 패키지

## 네이밍

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `InterviewController` |
| 메서드 | camelCase | `createInterview()` |
| 상수 | UPPER_SNAKE_CASE | `MAX_QUESTIONS` |
| DB 테이블 | snake_case, 단수형 | `interview`, `interview_question` |
| DB 컬럼 | snake_case | `interview_type`, `created_at` |
| 패키지 | 소문자 | `com.rehearse.api.domain.interview` |

### Entity 네이밍 패턴

- 기본: 도메인 명사 (`Interview`, `Question`, `User`)
- **예외**: 동일 패키지에 record VO와 JPA Entity가 충돌할 때만 `*Entity` 접미사 허용
  - 예: `RubricScore` (record VO) + `RubricScoreEntity` (JPA)
- 임의 `Entity` 접미사 추가 금지 — 충돌 회피 목적 외 사용 X

### 런타임 캐시 vs 영속 저장소 네이밍

| 역할 | 접미사 | 예시 |
|------|--------|------|
| DB 저장 담당 컴포넌트 | `*Persister` | `ResumeSkeletonPersister` |
| 인메모리 캐시 (Caffeine 등) 래퍼 | `*RuntimeCache` | `InterviewRuntimeStateCache` |
| Spring Cache 추상화 | `*Cache` (Spring `@Cacheable` 사용 시만) | — |

## Config 위치

| Config 유형 | 위치 | 예시 |
|------------|------|------|
| 도메인 무관 Spring Configuration | `global/config/` | `AsyncConfig`, `CorsConfig`, `RuntimeCacheConfig` |
| 인프라 자체 설정 | infra 하위 패키지 | `AwsConfig`, `GoogleTtsConfig` |
| 도메인 내부 config | **금지** — `global/config/` 로 이전 | — |

## Port (인터페이스) 위치 기준

| 분류 | 기준 | 위치 | 예시 |
|------|------|------|------|
| 인프라 정당 | 외부 SDK 추상화, 도메인 의존 0~1건 | `infra/` | `S3Service`, `TtsService` |
| 도메인 Port (후속 PR 대상) | 비즈니스 추상화, 다수 도메인이 의존 | `domain/{core}/` | `AiClient`, `SttService` |

> 현재(2026-04) `AiClient`/`SttService`는 infra 위치 유지. 도메인 이전은 별도 후속 PR.
> Service는 구현체가 아닌 **인터페이스**에 의존 (`private final AiClient aiClient` — 구현체 주입 금지).

## 계층 규칙

| 계층 | 책임 | 금지 사항 |
|------|------|----------|
| Controller | HTTP 처리, 입력 검증 (`@Valid`) | 비즈니스 로직, 직접 Repository 호출 |
| Service | 비즈니스 로직, 트랜잭션 | HTTP 관련 코드, 직접 DB 쿼리 |
| Repository | 데이터 접근 | 비즈니스 로직 |

## DTO 패턴

```java
// Request: @Getter + @NoArgsConstructor + Jakarta Validation
@Getter
@NoArgsConstructor
public class CreateInterviewRequest {
    @NotNull(message = "직무를 선택해주세요.")
    private Position position;
    // ...
}

// Response: @Getter + @Builder + static from() 팩토리 메서드
```

> 상세 패턴은 `CODING_GUIDE.md` 섹션 6 참조

## 에러 처리

- `BusinessException` + `ErrorCode` 인터페이스로 도메인별 예외 생성
- 에러 코드 체계: `{DOMAIN}_{3자리}` (예: `INTERVIEW_001`, `AI_001`)
- `GlobalExceptionHandler`에서 통일 에러 응답

## 응답 형식

```java
// 성공: ApiResponse.ok(data)
// 실패: ErrorResponse (GlobalExceptionHandler가 자동 생성)
```

## 테스트

- 서비스 단위 테스트 필수 (핵심 비즈니스 로직)
- `@MockitoBean`으로 외부 의존성 격리 (Spring Boot 3.4+, `@MockBean` deprecated)
- 테스트 메서드명: `메서드명_조건_기대결과` (예: `createInterview_success`)
