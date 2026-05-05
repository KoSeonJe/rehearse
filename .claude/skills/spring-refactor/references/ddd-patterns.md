# DDD 전술 패턴 레퍼런스 (Java 21 + Spring Boot 3.x)

## Aggregate 설계 규칙

### 핵심 원칙
- 트랜잭션당 하나의 Aggregate만 수정
- 다른 Aggregate는 **ID로만 참조** (객체 참조 X)
- Aggregate Root만 외부에 노출, 내부 Entity는 Root를 통해서만 접근

```java
// CORRECT: ID 참조
@Entity
public class Interview {
    private Long userId;  // User aggregate를 ID로 참조

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewQuestion> questions = new ArrayList<>();

    // Root가 컬렉션 관리
    public void addQuestion(InterviewQuestion question) {
        this.questions.add(question);
        question.assignInterview(this);
    }
}

// WRONG: 객체 참조 → tight coupling
public class Interview {
    @ManyToOne
    private User user;        // 다른 Aggregate 직접 참조
    @OneToOne
    private Feedback feedback; // 별도 Aggregate 직접 참조
}
```

### Aggregate 경계 판단 기준
- **같은 트랜잭션에서 반드시 함께 변경되어야 하는가?** → 같은 Aggregate
- **독립적으로 생성/삭제될 수 있는가?** → 별도 Aggregate
- **다른 사용자/프로세스가 동시에 수정할 수 있는가?** → 별도 Aggregate

---

## Entity vs Value Object

| 구분 | 식별성 | 변경성 | Java 관용구 |
|-----|-------|-------|------------|
| Entity | ID로 식별 | 도메인 메서드로 제어된 변경 | `@Entity` class |
| Value Object | 모든 필드의 값으로 식별 | 불변 (교체만 가능) | Java `record` |

### Value Object → Java Record

```java
// 단일 값 VO
public record Email(String value) {
    public Email {
        if (!value.matches("^[^@]+@[^@]+\\.[^@]+$"))
            throw new IllegalArgumentException("Invalid email: " + value);
    }
}

// 복합 VO
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount);
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Amount cannot be negative");
    }
    public Money add(Money other) {
        if (!this.currency.equals(other.currency))
            throw new IllegalArgumentException("Currency mismatch");
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

### JPA 연동 — AttributeConverter
```java
@Converter(autoApply = true)
public class EmailConverter implements AttributeConverter<Email, String> {
    @Override
    public String convertToDatabaseColumn(Email email) {
        return email == null ? null : email.value();
    }
    @Override
    public Email convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new Email(dbData);
    }
}
```

### JPA 연동 — @Embeddable (다중 컬럼 매핑)
```java
@Embeddable
public class Address {
    private String street;
    private String city;
    private String zipCode;
    protected Address() {} // JPA용
    public Address(String street, String city, String zipCode) {
        // 유효성 검증
        this.street = street;
        this.city = city;
        this.zipCode = zipCode;
    }
}
```

**주의:** `record`는 `@Entity`로 직접 사용 불가. VO로만 사용하고 `AttributeConverter` 또는 `@Embeddable`로 매핑.

---

## Rich Domain Model (Anemic → Rich 전환)

### BEFORE — Anemic (Transaction Script)
```java
// Service가 모든 로직 수행
@Service
public class InterviewService {
    public void completeInterview(Long id) {
        Interview interview = finder.findById(id);
        if (interview.getStatus() != InterviewStatus.IN_PROGRESS)
            throw new BusinessException(INVALID_STATUS_TRANSITION);
        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setCompletedAt(LocalDateTime.now());
        interviewRepository.save(interview);
    }
}
```

### AFTER — Rich Domain Model
```java
// Entity가 자기 불변식을 보호
@Entity
public class Interview extends AbstractAggregateRoot<Interview> {
    // @Setter 없음

    public void complete() {
        if (!status.canTransitionTo(InterviewStatus.COMPLETED))
            throw new BusinessException(INVALID_STATUS_TRANSITION);
        this.status = InterviewStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        registerEvent(new InterviewCompletedEvent(this.id));
    }
}

// Service는 thin orchestration만
@Service
public class InterviewService {
    @Transactional
    public void completeInterview(Long id) {
        Interview interview = finder.findById(id);
        interview.complete();  // Tell, Don't Ask
        // JPA dirty checking → save() 불필요
    }
}
```

### 전환 단계 (점진적)
1. `@Setter` 제거 → 도메인 메서드 도입 (가장 높은 ROI)
2. 상태 전이 로직을 Entity로 이동
3. 유효성 검증을 Entity 내부로 이동
4. 컬렉션 getter를 `Collections.unmodifiableList()`로 변경
5. 팩토리 메서드 도입 (`static create(...)`)
6. 불변식 검증을 생성자/팩토리에 집중

---

## Domain Events (Spring Data)

### Event 정의 — Java record
```java
public record InterviewCompletedEvent(Long interviewId, LocalDateTime completedAt) {}
```

### Aggregate에서 이벤트 등록
```java
@Entity
public class Interview extends AbstractAggregateRoot<Interview> {
    public void complete() {
        // ... 상태 전이 ...
        registerEvent(new InterviewCompletedEvent(this.id, this.completedAt));
    }
}
```

### 이벤트 핸들러 — AFTER_COMMIT에서 실행
```java
@Component
@RequiredArgsConstructor
public class InterviewEventHandler {
    private final ReportGenerationService reportService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InterviewCompletedEvent event) {
        reportService.generateAsync(event.interviewId());
    }
}
```

**주의:** `@DomainEvents`는 `save()`, `saveAll()`, `delete()`, `deleteAll()`에서만 발행. `deleteById()`는 쿼리 삭제라 발행 안 됨.

---

## Domain Service

여러 Aggregate에 걸치거나 어느 Entity에도 자연스럽게 속하지 않는 로직:

```java
@Service  // 도메인 레이어, stateless
public class InterviewScoringService {
    // Repository 주입 X — 순수 도메인 로직만
    public Score calculateScore(Interview interview, List<Feedback> feedbacks) {
        // 여러 aggregate 데이터를 조합하는 계산
    }
}
```

---

## Factory Pattern

```java
@Entity
public class Interview {
    public static Interview create(Long userId, Position position, 
                                    int durationMinutes, String resumeText) {
        validate(durationMinutes);
        return Interview.builder()
            .userId(userId)
            .position(position)
            .durationMinutes(durationMinutes)
            .resumeText(resumeText)
            .build();
    }

    private static void validate(int durationMinutes) {
        if (durationMinutes < 5 || durationMinutes > 120)
            throw new IllegalArgumentException("Duration must be 5-120 minutes");
    }

    @Builder(access = AccessLevel.PRIVATE)
    private Interview(Long userId, Position position, 
                      int durationMinutes, String resumeText) {
        this.userId = userId;
        this.position = position;
        this.durationMinutes = durationMinutes;
        this.resumeText = resumeText;
        this.status = InterviewStatus.READY;  // 불변식: 항상 READY로 시작
    }
}
```

---

## Enum 상태 머신

```java
public enum InterviewStatus {
    READY {
        @Override
        public boolean canTransitionTo(InterviewStatus target) {
            return target == IN_PROGRESS || target == CANCELLED;
        }
    },
    IN_PROGRESS {
        @Override
        public boolean canTransitionTo(InterviewStatus target) {
            return target == COMPLETED || target == CANCELLED;
        }
    },
    COMPLETED {
        @Override
        public boolean canTransitionTo(InterviewStatus target) {
            return false;  // 최종 상태
        }
    },
    CANCELLED {
        @Override
        public boolean canTransitionTo(InterviewStatus target) {
            return false;  // 최종 상태
        }
    };

    public abstract boolean canTransitionTo(InterviewStatus target);
}
```

---

## Java 21 특화 패턴

### Sealed Interface — 도메인 계층 구조
```java
public sealed interface AnalysisResult
    permits AnalysisResult.Success, AnalysisResult.Failed, AnalysisResult.Pending {
    record Success(List<FeedbackItem> feedbacks, String transcript) implements AnalysisResult {}
    record Failed(String errorCode, String reason) implements AnalysisResult {}
    record Pending(String jobId) implements AnalysisResult {}
}

// 컴파일러가 모든 케이스 처리 강제
return switch (result) {
    case Success s -> "완료: " + s.feedbacks().size() + "개 피드백";
    case Failed f  -> "실패: " + f.reason();
    case Pending p -> "진행 중 (jobId=" + p.jobId() + ")";
};
```

### Pattern Matching
```java
// instanceof → pattern matching
if (event instanceof InterviewCompletedEvent e) {
    handleCompleted(e);
}

// switch + guard
return switch (interview.getDurationMinutes()) {
    case int d when d <= 15  -> "SHORT";
    case int d when d <= 45  -> "MEDIUM";
    case int d when d <= 90  -> "LONG";
    default                  -> "EXTENDED";
};
```

---

## 안티패턴 요약

| 안티패턴 | 심각도 | 탐지 시그널 | 해결 |
|---------|--------|-----------|-----|
| Transaction Script | HIGH | Service에 비즈니스 로직, Entity에 행위 없음 | Rich Domain Model |
| Anemic Domain Model | HIGH | Entity = Getter + Setter + NoArgsCtor | 도메인 메서드 도입 |
| Public Setter | HIGH | `@Setter` 또는 `setXxx()` | 도메인 메서드로 대체 |
| Cross-Aggregate Reference | MEDIUM | `@ManyToOne` 다른 Aggregate | ID 참조로 전환 |
| Primitive Obsession | MEDIUM | String/Long이 도메인 개념 | Value Object (record) |
| Exposed Mutable Collection | MEDIUM | `getList()` → mutable | `unmodifiableList()` + 도메인 메서드 |
| Missing Domain Events | LOW | 상태 전이 후 다른 Service 직접 호출 | `AbstractAggregateRoot` + Event |
