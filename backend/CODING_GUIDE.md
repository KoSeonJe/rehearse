# Backend Coding Guide

> **How** to write code. For naming/directory/tooling rules, see `CONVENTIONS.md`.

---

## 1. Architecture & Layering

### Dependency Direction

```
Controller → Service → Repository → Entity
                         ↓
                   infra (via interface)
```

**Reverse dependency is banned.** Service must not know Controller; Repository must not know Service.

| Layer | Responsibility | Banned |
|-------|---------------|--------|
| Controller | HTTP request/response, `@Valid` | Business logic, direct Repository call |
| Service | Business orchestration, transactions | HTTP-related code |
| Repository | Data access, query definition | Business logic |
| Entity | Domain state + domain behavior | DTO conversion, external calls |
| infra | External system integration | Domain logic |

### No Direct Dependency on infra Implementations

Services depend on interfaces, not concrete classes. `AiClient` interface lives in `infra.ai` — services import the interface, never `ClaudeApiClient` directly.

```java
// ❌ Depends on implementation
private final ClaudeApiClient claudeApiClient;

// ✅ Depends on interface — implementations are swappable
private final AiClient aiClient;
```

---

## 2. Clean Code

### Method Size Limits

| Type | Max Lines | Action |
|------|-----------|--------|
| Controller method | 15 | Move logic to Service |
| Service method | 30 | Extract private helpers |
| Private helper | 20 | Extract to separate class |

### No Magic Numbers/Strings

```java
// ❌
if (request.getDurationMinutes() > 120) { ... }

// ✅
private static final int MAX_DURATION_MINUTES = 120;
if (request.getDurationMinutes() > MAX_DURATION_MINUTES) { ... }
```

### Optional Rules

| Rule | Description |
|------|-------------|
| Return type only | Never use Optional as parameter or field |
| `.get()` banned | Use `orElseThrow()`, `orElse()`, `ifPresent()` |
| No `.isPresent()` + `.get()` | Use `map()`, `orElseGet()` chaining |

### Early Return Pattern

```java
// ❌ Deep nesting
if (interview.getStatus() == IN_PROGRESS) {
    // ... happy path
} else {
    throw new BusinessException(InterviewErrorCode.NOT_IN_PROGRESS);
}

// ✅ Guard clause first
if (interview.getStatus() != IN_PROGRESS) {
    throw new BusinessException(InterviewErrorCode.NOT_IN_PROGRESS);
}
// ... happy path
```

### 3+ Parameters → Wrap in DTO

---

## 3. OOP Principles

### SOLID in This Project

| Principle | Project Example |
|-----------|----------------|
| **SRP** | `InterviewFinder` (query only) vs `InterviewService` (business logic) |
| **OCP** | `AiClient` interface — swap `ClaudeApiClient` ↔ `MockAiClient` without changing Service |
| **LSP** | `MockAiClient` honors all `AiClient` contracts identically |
| **ISP** | `ErrorCode` interface — only `getStatus()`, `getCode()`, `getMessage()` |
| **DIP** | `InterviewService` → `AiClient`(interface) ← `ClaudeApiClient`(impl) |

### Law of Demeter: No Deep Chaining

```java
// ❌ Exposes internal structure
String category = interview.getQuestions().get(0).getCategory();

// ✅ Delegation method
String category = interview.getFirstQuestionCategory();
```

### Composition over Inheritance

Use Finder pattern (injected `@Component`) instead of abstract base classes.

---

## 4. Entity Design

### Template

```java
@Entity
@Table(name = "interview")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Interview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // fields ...
    @CreatedDate @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Interview(/* required params */) {
        this.status = InterviewStatus.READY;
    }
    // domain behavior methods ...
}
```

| Element | Rule |
|---------|------|
| `@NoArgsConstructor` | `AccessLevel.PROTECTED` — prevent empty object creation |
| `@Builder` | On constructor (not class) — enforce required fields |
| `@Getter` | Allowed. `@Setter` **banned** |

### Enum Mapping

`@Enumerated(EnumType.STRING)` required. **ORDINAL banned** — reordering breaks data.

### Associations

| Rule | Description |
|------|-------------|
| `FetchType.LAZY` default | All associations including `@ManyToOne`. EAGER banned |
| Fetch Join | Use `@Query` + `JOIN FETCH` in Repository when needed |
| `cascade` | Parent-child only (`CascadeType.ALL` + `orphanRemoval`) |

### State Machine: Encapsulate Transitions in Enum

```java
public enum InterviewStatus {
    READY, IN_PROGRESS, COMPLETED;

    public boolean canTransitionTo(InterviewStatus target) {
        return switch (this) {
            case READY -> target == IN_PROGRESS;
            case IN_PROGRESS -> target == COMPLETED;
            case COMPLETED -> false;
        };
    }
}

// Entity validates transitions
public void updateStatus(InterviewStatus newStatus) {
    if (!this.status.canTransitionTo(newStatus))
        throw new IllegalStateException("Cannot transition " + status + " → " + newStatus);
    this.status = newStatus;
}
```

### Domain Logic Lives in Entity

```java
// ❌ Service manipulates entity internals
interview.getQuestions().add(question);
question.setInterview(interview);

// ✅ Entity method encapsulates behavior
public void addQuestion(InterviewQuestion question) {
    this.questions.add(question);
    question.assignInterview(this);
}
```

### Entity Must Not Create DTOs

DTO conversion belongs in `Response.from(entity)`, not `entity.toResponse()`.

---

## 5. Service Layer

### Standard Annotations

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // class default
public class InterviewService {

    @Transactional                 // write override
    public InterviewResponse createInterview(...) { ... }

    public InterviewResponse getInterview(Long id) { ... } // inherits readOnly
}
```

### Finder Pattern: Shared Query Logic

```java
@Component
@RequiredArgsConstructor
public class InterviewFinder {
    private final InterviewRepository interviewRepository;

    public Interview findById(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new BusinessException(InterviewErrorCode.NOT_FOUND));
    }
}
```

**Why?** `InterviewService`, `FeedbackService`, `ReportService` all need Interview lookups. Finder centralizes query + error handling.

### Cross-Domain Access: Via Finder, Not Repository

```java
// ❌ Cross-domain Repository injection
private final InterviewRepository interviewRepository; // in FeedbackService

// ✅ Inject Finder
private final InterviewFinder interviewFinder;
```

### Business Logic Placement

| Logic Type | Location |
|-----------|----------|
| State validation/transition | Entity |
| Single entity rules | Entity |
| Cross-entity orchestration | Service |
| External system calls | Service (delegated to infra) |

### Service 계층 세분화

같은 `service/` 디렉토리 안에 두 종류의 클래스가 공존한다. 클래스명으로 구분한다.

| 종류 | 기준 | 클래스명 접미사 | 예시 |
|------|------|----------------|------|
| 애플리케이션 Service | use case 진입점. `@Service` + `@Transactional`, 외부 협력 OK | `*Service`, `*QueryService` | `InterviewService`, `InterviewQueryService` |
| 도메인 Service (rare) | 순수 비즈니스 규칙. 트랜잭션 X, 외부 협력 X. Entity 메서드로 충분하지 않을 때만 | `*Policy`, `*Calculator`, `*Classifier` | `StandardFollowUpPolicy`, `IntentClassifier` |
| 공유 조회 컴포넌트 | 여러 Service에서 재사용하는 조회 + 예외 변환. `@Component` | `*Finder` | `InterviewFinder` |
| DB 저장 컴포넌트 | 복잡한 영속화 로직 캡슐화. `@Component` + `@Transactional` | `*Persister` | `ResumeSkeletonPersister` |
| 인메모리 캐시 래퍼 | Caffeine 등 로컬 캐시 읽기/쓰기. `@Component` | `*RuntimeCache` | `InterviewRuntimeStateCache` |

**Strategy 패턴**: interface + 구현체 여럿 → 모두 `service/` 안에 위치.
별도 `policy/` 패키지 생성 금지.

---

## 6. DTO Patterns

### Request DTO

`@Getter` + `@NoArgsConstructor` + Jakarta Validation with Korean messages.

```java
@Getter
@NoArgsConstructor
public class CreateInterviewRequest {
    @NotNull(message = "직무를 선택해주세요.")
    private Position position;
    @Min(value = 5, message = "면접 시간은 최소 5분입니다.")
    private Integer durationMinutes;
}
```

### Response DTO

`@Getter` + `@Builder` + `static from(Entity)` factory method.

```java
@Getter @Builder
public class InterviewResponse {
    private final Long id;
    private final Position position;

    public static InterviewResponse from(Interview interview) {
        return InterviewResponse.builder()
                .id(interview.getId())
                .position(interview.getPosition())
                .build();
    }
}
```

### Rules

- **Entity direct return banned** — always convert to Response DTO
- **infra DTOs separated** — `infra/ai/dto/` for AI response types, `domain/*/dto/` for domain types

---

## 7. Error Handling

### ErrorCode Interface + Domain Enums

```java
// Interface
public interface ErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}

// Domain enum
@Getter @RequiredArgsConstructor
public enum InterviewErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "INTERVIEW_002", "잘못된 상태 전이입니다.");
    // ...
}
```

### Error Code Scheme: `{DOMAIN}_{3digits}`

`INTERVIEW_001`, `FEEDBACK_001`, `REPORT_001`, `AI_001`

### Logging Levels

| Level | Target |
|-------|--------|
| `warn` | Business exceptions, validation failures |
| `error` | System errors, unhandled exceptions |
| `info` | Normal flow milestones |

### External API Error Wrapping

Wrap external exceptions in `BusinessException` — never leak internal details to client.

---

## 8. Repository & Data Access

### N+1 Prevention: JOIN FETCH

```java
@Query("SELECT i FROM Interview i JOIN FETCH i.questions WHERE i.id = :id")
Optional<Interview> findByIdWithQuestions(@Param("id") Long id);
```

### Method Naming

| Pattern | Use |
|---------|-----|
| `findBy...` | Conditional query |
| `findByIdWith...` | Fetch Join query |
| `existsBy...` | Existence check |

### Bulk Save: `saveAll()` not loop `save()`

```java
// ❌ N individual INSERTs
for (Feedback f : feedbacks) { feedbackRepository.save(f); }

// ✅ Batch
feedbackRepository.saveAll(feedbacks);
```

---

## 9. Test Strategy

### Pyramid: Unit > Slice > Integration

| Level | Annotation | Target |
|-------|-----------|--------|
| Unit | `@ExtendWith(MockitoExtension.class)` | Service, Entity logic |
| Slice | `@WebMvcTest` | Controller (HTTP, validation) |
| Integration | `@SpringBootTest` | Full flow (minimal) |

### Structure: Given-When-Then + BDDMockito

```java
@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {
    @InjectMocks private InterviewService interviewService;
    @Mock private InterviewRepository interviewRepository;
    @Mock private AiClient aiClient;

    @Test
    @DisplayName("면접 세션 생성 시 Claude API로 질문을 생성하고 저장한다")
    void createInterview_success() {
        // given
        given(aiClient.generateQuestions(...)).willReturn(mockQuestions);
        // when
        InterviewResponse response = interviewService.createInterview(request, null);
        // then
        assertThat(response.getId()).isEqualTo(1L);
        then(interviewRepository).should().save(any(Interview.class));
    }
}
```

### Rules

- **`@MockitoBean`** for Spring Boot 3.4+ (`@MockBean` deprecated)
- **Method name**: `methodName_scenario_expectedResult`
- **`@DisplayName`**: Korean description of test intent
- **Helper methods**: `createMock...()` pattern for repeated mock objects
- **Exception verification**: `assertThatThrownBy()` + `.satisfies()`

---

## 10. API Design

### URL Rules

`/api/v1/{plural-resource}` — nouns only, kebab-case, no verbs.

### Response: `ApiResponse<T>` Wrapper

All success responses wrapped in `ApiResponse.ok(data)`.

### HTTP Status Codes

| Scenario | Code |
|----------|------|
| Created | `201` |
| Read/Update | `200` |
| Delete | `204` |
| Validation error | `400` |
| Not found | `404` |
| State conflict | `409` |
| External API failure | `502` |

### `@Valid` Position

Controller parameters only. Never validate manually in Service.

---

## 11. Infra Layer

### Interface + Conditional Bean Loading

```java
public interface AiClient { ... }

@Component
@ConditionalOnExpression("!'${claude.api-key:}'.isEmpty()")
public class ClaudeApiClient implements AiClient { ... }

@Component
@ConditionalOnMissingBean(ClaudeApiClient.class)
public class MockAiClient implements AiClient { ... }
```

### Responsibility Separation

`QuestionGenerationPromptBuilder` / `FollowUpPromptBuilder` (prompt construction) + `ClaudeApiClient` (API call) + `ClaudeResponseParser` (response parsing) — each change independently.

---

## 12. Performance

> Lazy Loading, Fetch Join, saveAll rules: see §4 (Entity) and §8 (Repository).

### Parameterized Logging

```java
// ❌ String concatenation — always computed
log.info("Created: id=" + id + ", pos=" + position);

// ✅ Parameterized — only computed when log level matches
log.info("Created: id={}, position={}", id, position);
```

### Avoid Unnecessary Queries

Use `existsById()` for existence checks instead of loading full entity.

---

## 13. Code Review Checklist

### Architecture
- [ ] Controller → Service → Repository layering respected
- [ ] No direct Repository call from Controller
- [ ] No direct infra implementation dependency from domain
- [ ] Cross-domain access via Finder, not Repository

### Code Quality
- [ ] No magic numbers/strings
- [ ] Method size within limits
- [ ] Optional used correctly (no `.get()`, return type only)
- [ ] Early return pattern, minimal nesting
- [ ] No `@Setter` — domain methods for state changes

### Data
- [ ] Enum: `EnumType.STRING` (not ORDINAL)
- [ ] Associations: `FetchType.LAZY` default
- [ ] No N+1 (Fetch Join or `@EntityGraph`)
- [ ] Bulk: `saveAll()` not loop `save()`

### Security
- [ ] No Entity direct return in API response
- [ ] Validation via `@Valid` in Controller
- [ ] External API errors wrapped in `BusinessException`

### Tests
- [ ] Service unit test exists
- [ ] Given-When-Then structure
- [ ] `@DisplayName` present
- [ ] Exception cases covered
- [ ] `@MockitoBean` (not `@MockBean`)

### Performance
- [ ] Parameterized logging (`log.info("id={}", id)`)
- [ ] Read methods: `@Transactional(readOnly = true)`
