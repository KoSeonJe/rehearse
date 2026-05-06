# OOP / SOLID 원칙 레퍼런스 (Java Spring 맥락)

## SOLID 원칙

### S — Single Responsibility Principle (단일 책임)

클래스는 **변경의 이유가 하나**여야 한다. "하나의 일"이 아니라 "하나의 변경 이유"에 집중한다.

```java
// BAD: God Service — 변경 이유 5개 (생성, 조회, 상태관리, AI호출, 리포트)
@Service
public class InterviewService {
    public Interview create(...) { ... }
    public InterviewResponse findById(...) { ... }
    public void updateStatus(...) { ... }
    public List<String> generateQuestions(...) { ... }
    public Report generateReport(...) { ... }
}

// GOOD: 책임별 분리
InterviewCreationService   // 생성 로직 변경 시
InterviewQueryService      // 조회/필터 요구사항 변경 시
InterviewProgressService   // 상태 전이 규칙 변경 시
QuestionGenerationService  // AI 질문 생성 전략 변경 시
ReportGenerationService    // 리포트 형식/내용 변경 시
```

**탐지 기준:**
- Service에 15개 이상 public 메서드
- Service에 5개 이상 의존성 주입
- 메서드들을 2개 이상 그룹으로 나눌 수 있음

### O — Open/Closed Principle (개방-폐쇄)

확장에 열려 있고, 수정에 닫혀 있어야 한다. 새로운 유형 추가 시 기존 코드 변경 없이 가능해야 한다.

```java
// BAD: 새 AI 제공자 추가마다 if문 추가
public List<String> generateQuestions(String provider, String prompt) {
    if ("claude".equals(provider)) { ... }
    else if ("openai".equals(provider)) { ... }
    else if ("gemini".equals(provider)) { ... }  // 추가할 때마다 수정
}

// GOOD: 인터페이스 + 구현체 추가로 확장
public interface AiClient {
    List<String> generateQuestions(QuestionGenerationRequest request);
}

@Component("claude")
public class ClaudeAiClient implements AiClient { ... }

@Component("openai")
public class OpenAiClient implements AiClient { ... }

// 새 제공자 = 새 클래스 추가, 기존 코드 수정 0
```

**Spring에서의 적용:**
- Strategy Pattern + `@Component` + `Map<String, T>` 자동 주입
- Template Method Pattern: `AbstractService` + 구체 구현

### L — Liskov Substitution Principle (리스코프 치환)

하위 타입은 상위 타입의 **계약(contract)**을 완전히 준수해야 한다.

```java
// BAD: 하위 타입이 계약 위반
public class ReadOnlyRepository extends InterviewRepository {
    @Override
    public Interview save(Interview interview) {
        throw new UnsupportedOperationException();  // 계약 위반!
    }
}

// GOOD: 인터페이스 분리로 해결
public interface InterviewReader {
    Interview findById(Long id);
    List<Interview> findAll();
}
public interface InterviewWriter extends InterviewReader {
    Interview save(Interview interview);
}
```

### I — Interface Segregation Principle (인터페이스 분리)

클라이언트가 사용하지 않는 메서드에 의존하지 않아야 한다.

```java
// BAD: 뚱뚱한 인터페이스
public interface AiClient {
    List<String> generateQuestions(QuestionRequest request);
    String generateFeedback(FeedbackRequest request);
    String generateReport(ReportRequest request);
    byte[] textToSpeech(String text);
    String translateText(String text, String targetLang);
}

// GOOD: 역할별 분리
public interface QuestionGenerator {
    List<String> generateQuestions(QuestionRequest request);
}
public interface FeedbackGenerator {
    String generateFeedback(FeedbackRequest request);
}
public interface TextToSpeechClient {
    byte[] synthesize(String text);
}
```

### D — Dependency Inversion Principle (의존성 역전)

고수준 모듈이 저수준 모듈에 직접 의존하지 않고, 둘 다 추상화에 의존한다.

```java
// BAD: 구체 클래스에 직접 의존
@Service
public class QuestionGenerationService {
    private final ClaudeApiClient claudeClient;  // 구체 클래스
}

// GOOD: 인터페이스에 의존
@Service
public class QuestionGenerationService {
    private final AiClient aiClient;  // 인터페이스 → 테스트 시 Mock 교체 가능
}
```

**Spring에서의 자연스러운 적용:**
- `@RequiredArgsConstructor` + `final` 필드 = 생성자 주입
- 인터페이스 타입으로 선언, `@Component`/`@Service`로 구현체 등록
- `@Qualifier` 또는 `@Primary`로 구현체 선택

---

## 핵심 OOP 원칙

### Tell, Don't Ask (명령하라, 묻지 마라)

객체의 상태를 꺼내서 판단하지 말고, 객체에게 행위를 요청하라.

```java
// BAD (Ask): 상태를 물어보고 외부에서 결정
if (interview.getStatus() == InterviewStatus.READY) {
    if (interview.getQuestions() != null && !interview.getQuestions().isEmpty()) {
        interview.setStatus(InterviewStatus.IN_PROGRESS);
        interview.setStartedAt(LocalDateTime.now());
    }
}

// GOOD (Tell): 객체에게 명령
interview.start();  // Entity가 내부에서 전제조건 검증 + 상태 전이
```

### 캡슐화 — 컬렉션 보호

```java
// BAD: 외부에서 컬렉션 조작 가능
public List<Question> getQuestions() {
    return questions;  // mutable 참조 노출
}
// 호출자: interview.getQuestions().clear();  // Entity 불변식 파괴

// GOOD: 불변 뷰 + 도메인 메서드로 조작
public List<Question> getQuestions() {
    return Collections.unmodifiableList(questions);
}
public void addQuestion(Question question) {
    validateCanAddQuestion();
    this.questions.add(question);
    question.assignInterview(this);
}
```

### Law of Demeter (디미터 법칙)

메서드 체인을 통해 "낯선" 객체에 접근하지 않는다.

```java
// BAD: 3단계 이상 탐색
interview.getQuestionSet().getQuestions().get(0).getCategory().getDisplayName();

// GOOD: 위임 메서드
interview.getFirstQuestionCategoryName();
// 또는 필요한 정보만 DTO로 조회
```

### 다형성으로 조건문 대체

```java
// BAD: 타입별 if/switch 분기
public String buildPrompt(Interview interview) {
    if (interview.getPosition() == BACKEND) {
        return "백엔드 면접 질문을 생성해주세요...";
    } else if (interview.getPosition() == FRONTEND) {
        return "프론트엔드 면접 질문을 생성해주세요...";
    } else if (interview.getPosition() == DEVOPS) {
        return "데브옵스 면접 질문을 생성해주세요...";
    }
}

// GOOD 1: Enum에 행위 위임
public enum Position {
    BACKEND {
        @Override
        public String buildInterviewPrompt(String resumeText) {
            return "백엔드 개발자 면접입니다. 이력서: " + resumeText;
        }
    },
    FRONTEND {
        @Override
        public String buildInterviewPrompt(String resumeText) {
            return "프론트엔드 개발자 면접입니다. 이력서: " + resumeText;
        }
    };
    
    public abstract String buildInterviewPrompt(String resumeText);
}

// GOOD 2: Strategy Pattern (로직이 복잡할 때)
public interface PromptStrategy {
    String buildPrompt(Interview interview);
    boolean supports(Position position);
}
```

**판단 기준:**
- 분기가 2~3개이고 로직이 1~2줄 → Enum에 행위 위임
- 분기가 4개 이상이거나 로직이 복잡 → Strategy Pattern
- 분기가 변경 가능성 낮고 간단 → 그냥 switch도 OK (과도한 추상화 경계)

### 상속보다 합성 (Composition over Inheritance)

```java
// BAD: 상속으로 코드 재사용
public class InterviewService extends BaseService<Interview> { ... }

// GOOD: 합성으로 재사용
@Service
@RequiredArgsConstructor
public class InterviewService {
    private final InterviewFinder finder;      // 조회 로직 위임
    private final InterviewValidator validator; // 검증 로직 위임
}
```

---

## 안티패턴 요약

| 안티패턴 | 위반 원칙 | 탐지 | 해결 |
|---------|----------|------|-----|
| God Class | SRP | 15+ 메서드, 5+ 의존성 | 책임별 분리 |
| Feature Envy | 캡슐화 | getX() 체인 → 외부 계산 → setX() | Tell, Don't Ask |
| Shotgun Surgery | SRP | 하나의 변경이 여러 클래스 수정 유발 | 관련 로직 한 곳에 모으기 |
| Inappropriate Intimacy | 디미터 법칙 | private 필드 직접 접근, 3단 체인 | 위임 메서드, DTO |
| Refused Bequest | LSP | 상속 후 메서드 빈 구현/예외 | 인터페이스 분리 |
| Parallel Inheritance | OCP | 클래스 추가 시 다른 계층도 추가 필요 | 합성 패턴 |
