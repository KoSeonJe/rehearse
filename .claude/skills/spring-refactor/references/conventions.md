# Java Spring 코드 컨벤션 레퍼런스

## 네이밍 규칙

### 클래스명
| 유형 | 패턴 | 예시 |
|-----|------|-----|
| Entity | 도메인 명사 (단수) | `Interview`, `Question`, `User` |
| Controller | `{도메인}Controller` | `InterviewController` |
| Service | `{역할}Service` | `InterviewQueryService`, `InterviewCreationService` |
| Repository | `{Entity}Repository` | `InterviewRepository` |
| DTO (요청) | `{동작}{도메인}Request` | `CreateInterviewRequest` |
| DTO (응답) | `{도메인}{내용}Response` | `InterviewListResponse` |
| Exception | `{도메인}ErrorCode` | `InterviewErrorCode` |
| Config | `{기능}Config` | `CorsConfig`, `AsyncConfig` |
| VO | 도메인 개념 명사 | `Email`, `Money`, `QuestionDistribution` |
| Enum | 도메인 개념 명사 | `InterviewStatus`, `Position` |

### 메서드명
| 레이어 | 패턴 | 예시 |
|-------|------|-----|
| Controller | HTTP 동사 기반 | `createInterview()`, `getInterview()`, `updateStatus()` |
| Service (명령) | 동사 + 목적어 | `startInterview()`, `completeInterview()`, `generateQuestions()` |
| Service (조회) | `find/get/query` + 조건 | `findById()`, `getInterviewStats()`, `queryByStatus()` |
| Repository | Spring Data 네이밍 | `findByUserIdAndStatus()`, `existsByEmail()` |
| Entity (행위) | 도메인 동사 | `complete()`, `start()`, `addQuestion()`, `cancel()` |
| Entity (팩토리) | `create`, `of`, `from` | `Interview.create(...)`, `Email.of(...)` |

### 변수명
- **boolean**: `is/has/can/should` 접두사 — `isCompleted`, `hasQuestions`, `canStart`
- **컬렉션**: 복수형 — `questions`, `feedbacks`, `users`
- **단일 객체**: 단수형 — `interview`, `question`, `user`
- **ID**: `{도메인}Id` — `interviewId`, `userId` (단, 자기 자신 ID는 `id`)
- **상수**: `UPPER_SNAKE_CASE` — `MAX_DURATION_MINUTES`, `DEFAULT_PAGE_SIZE`

### 패키지명
```
com.rehearse.api
├── domain
│   └── {도메인명}         # interview, questionset, user, ...
│       ├── controller
│       ├── service
│       ├── repository
│       ├── entity
│       ├── dto
│       ├── vo            # Value Objects
│       └── exception
└── global
    ├── common            # ApiResponse, ErrorResponse
    ├── config            # 전역 설정
    ├── exception         # GlobalExceptionHandler
    └── security          # JWT, OAuth2
```

## Spring 관용구

### Controller 규칙
```java
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {
    private final InterviewCreationService creationService;  // 구체적인 Service 이름

    @PostMapping
    public ApiResponse<InterviewResponse> createInterview(
            @Valid @RequestBody CreateInterviewRequest request,  // @Valid 필수
            @AuthenticationPrincipal CustomOAuth2User user) {    // 인증 정보는 파라미터로
        return ApiResponse.success(creationService.create(request, user.getUserId()));
    }
}
```

**Controller에서 하면 안 되는 것:**
- 비즈니스 로직 (if문으로 상태 판단 등)
- 직접 Repository 호출
- Entity 직접 반환 (DTO로 변환해야 함)
- try-catch (GlobalExceptionHandler가 처리)

### Service 규칙
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본 readOnly, 변경 메서드만 @Transactional
public class InterviewQueryService {
    private final InterviewFinder interviewFinder;  // 조회 전용 컴포넌트 활용

    public InterviewResponse findById(Long interviewId, Long userId) {
        Interview interview = interviewFinder.findByIdAndUserId(interviewId, userId);
        return InterviewResponse.from(interview);
    }
}
```

### DTO 규칙
```java
// 요청 DTO — record + Jakarta Validation
public record CreateInterviewRequest(
    @NotNull(message = "포지션은 필수입니다")
    Position position,

    @Min(value = 5, message = "면접 시간은 최소 5분입니다")
    @Max(value = 120, message = "면접 시간은 최대 120분입니다")
    int durationMinutes
) {}

// 응답 DTO — record + 정적 팩토리
public record InterviewResponse(
    Long id,
    String position,
    InterviewStatus status,
    LocalDateTime createdAt
) {
    public static InterviewResponse from(Interview interview) {
        return new InterviewResponse(
            interview.getId(),
            interview.getPosition().name(),
            interview.getStatus(),
            interview.getCreatedAt()
        );
    }
}
```

### Lombok 사용 규칙
| 허용 | 비허용 |
|-----|--------|
| `@RequiredArgsConstructor` (DI용) | `@Setter` (Entity에서 특히 금지) |
| `@Getter` (Entity 읽기 전용) | `@Data` (equals/hashCode 문제) |
| `@Builder` (private + 팩토리 메서드와 조합) | `@AllArgsConstructor` (필드 순서 의존) |
| `@Slf4j` | `@ToString` (연관 Entity lazy loading 문제) |

### 에러 처리 규칙
```java
// ErrorCode enum으로 통합 관리
public enum InterviewErrorCode implements ErrorCode {
    INTERVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "면접을 찾을 수 없습니다"),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 상태 전이입니다");

    private final HttpStatus status;
    private final String message;
}

// BusinessException으로 던지기
throw new BusinessException(InterviewErrorCode.INTERVIEW_NOT_FOUND);
```

## 안티패턴 시그널

- **Controller에 `if`문**: 비즈니스 로직이 Controller에 있다는 신호
- **Service에서 다른 Service 3개 이상 주입**: God Service 또는 책임 불명확
- **DTO 없이 Entity 직접 반환**: API 스펙과 도메인 모델 결합
- **`@Autowired` 필드 주입**: 테스트 어려움, `@RequiredArgsConstructor` 사용
- **`Optional.get()` 직접 호출**: `orElseThrow()` 사용
- **`new` 키워드로 의존성 생성**: DI 미활용
