# Backend 테스트 전략

> 이 프로젝트에서 **왜, 무엇을, 어떤 순서로** 테스트하는지 정의한다.
> 테스트 **작성법**(HOW)은 `CONVENTIONS.md` 및 `.claude/skills/spring-test/references/` 참조.

---

## 목적

테스트는 커버리지 숫자를 올리기 위한 것이 아니다. 이 프로젝트에서 테스트의 목적은 3가지다:

1. **회귀 방지** — 기존 기능이 변경 후에도 동작함을 보장
2. **리팩토링 안전망** — 내부 구조 변경 시 외부 행위가 깨지지 않음을 검증
3. **의도 문서화** — 코드가 "왜 이렇게 동작하는지" 테스트로 설명

---

## 테스트 대상 판단 기준

### 반드시 테스트하는 것

| 대상 | 이유 | 테스트 유형 |
|------|------|-----------|
| Service 비즈니스 로직 | 핵심 가치, 회귀 위험 최대 | Unit |
| Entity 상태 전이 / 도메인 규칙 | 불변식 위반 시 데이터 오염 | Unit |
| Controller HTTP 계약 | API 소비자와의 약속 | Slice (`@WebMvcTest`) |
| 커스텀 Repository 쿼리 | JPQL/Native 쿼리 오류는 런타임에만 발견 | Slice (`@DataJpaTest`) |
| Finder 조회 + 예외 변환 | 도메인 간 접점, 예외 전파 경로 | Unit |
| 외부 API 클라이언트 에러 핸들링 | 장애 전파 방지 | Unit |

### 테스트하지 않는 것

| 대상 | 이유 |
|------|------|
| Getter/Setter, Builder | 프레임워크가 생성, 검증 가치 없음 |
| 단순 위임 메서드 (로직 없는 pass-through) | 호출만 전달, 상위에서 검증 |
| DTO `from()` 변환 (필드 매핑만 하는 경우) | Service 테스트에서 간접 검증 |
| ErrorCode enum 값 나열 | 상수 정의, 행위 없음 |
| Spring Configuration 클래스 | 빈 등록만, `@SpringBootTest` context load로 충분 |
| 단순 CRUD Repository (JPA 기본 메서드) | Spring Data JPA가 검증 완료 |

### 판단이 애매할 때

> **"이 코드가 깨지면 사용자가 바로 영향을 받는가?"**
>
> Yes → 테스트 작성. No → 상위 레이어 테스트에서 간접 검증.

---

## 우선순위 체계

### Tier 1: 필수 (PR 머지 조건)

새로 작성하거나 수정한 코드에 대해 아래를 반드시 포함:

- Service의 모든 public 메서드: **happy path + 대표 예외 경로 최소 1개**
- Controller: **정상 응답 + 인증 실패 + 유효성 검증 실패**
- Entity 상태 전이: **허용 전이 + 거부 전이**

### Tier 2: 권장 (기능 완성도)

- Service 메서드별 경계값, 엣지 케이스
- Repository 커스텀 쿼리 정확성
- Finder의 not-found 예외 변환
- DTO 변환 중 복잡한 로직이 있는 경우

### Tier 3: 선택 (시간 여유 시)

- 외부 클라이언트 재시도/서킷브레이커 시나리오
- 동시성 제어 (Lock) 시나리오
- `@SpringBootTest` 통합 플로우

---

## 도메인별 테스트 전략

### 핵심 도메인 (Interview, QuestionSet)

이 프로젝트의 핵심 가치. 가장 높은 커버리지 목표.

```
목표: Service 메서드 커버리지 90%+
필수: 모든 상태 전이 경로, AI 연동 실패 핸들링
주의: QuestionGeneration 파이프라인은 이벤트 기반 — 이벤트 발행/소비 각각 테스트
```

### 지원 도메인 (ReviewBookmark, ServiceFeedback, User, File)

핵심 도메인을 보조하는 기능. 적절한 수준의 커버리지.

```
목표: Service 메서드 커버리지 80%+
필수: CRUD 정상 동작, 권한 검증 (본인 소유 리소스만)
```

### 인프라 레이어 (AI Client, S3, TTS)

외부 시스템과의 경계. 모킹 전략이 중요.

```
목표: 에러 핸들링 경로 100% 커버
필수: API 호출 실패, 타임아웃, 잘못된 응답 형식 처리
전략: 실제 외부 호출은 하지 않음 — Mock/Stub으로 응답 시뮬레이션
```

### 글로벌/보안 (Security, ExceptionHandler, Filter)

횡단 관심사. 한 번 잘 테스트하면 안정적.

```
목표: 인증/인가 시나리오 커버
필수: JWT 생성/검증, OAuth2 핸들러, 예외 응답 형식
```

---

## 테스트 유형 선택 규칙

```
질문: "Spring 컨텍스트가 없어도 검증 가능한가?"
  ├─ Yes → Unit Test
  └─ No
       ├─ HTTP 계층만 필요 → @WebMvcTest
       ├─ JPA/DB만 필요 → @DataJpaTest
       └─ 전체 필요 → @SpringBootTest (최후의 수단)
```

**원칙: 가장 가벼운 테스트 유형을 먼저 선택한다.**

| 유형 | 실행 속도 | 사용 비율 | 용도 |
|------|----------|----------|------|
| Unit | ~1ms/test | 70% | Service, Entity, Finder, VO, Utility |
| Slice | ~200ms (컨텍스트) | 20% | Controller, Repository |
| Integration | ~1500ms (컨텍스트) | 10% | 레이어 간 상호작용, 트랜잭션 전파 |

---

## Mock 정책

### Mock 대상 (외부 경계)

- Repository (Unit Test에서)
- 외부 API 클라이언트 (AiClient, S3Service, TtsService 등 인터페이스)
- Finder (타 도메인 접근)
- 시간(`Clock`), 랜덤(`Random`) 등 비결정적 요소

### Mock 금지 (내부 로직)

- Entity 생성 및 도메인 메서드
- DTO 변환
- 값 객체 (VO)
- 테스트 대상 클래스 자체

### Over-Mocking 판단 기준

아래 중 하나라도 해당하면 테스트 설계를 재검토:

- `given()` 설정이 5줄 이상
- `then().should()` 검증이 3개 이상
- Mock 설정 코드가 실제 검증 코드보다 김

→ 대상 클래스의 책임이 과도하거나, Integration Test가 더 적합한 신호.

---

## CI 연동 규칙

### PR 머지 조건

```
Backend CI: ./gradlew test 전체 통과 필수
```

- 새 Service/Controller 추가 시 → 대응 테스트 파일 필수
- 기존 로직 수정 시 → 관련 테스트가 변경을 반영하거나 그대로 통과

### 로컬 개발 시

```bash
# 작업 중인 도메인만 빠르게 검증
./gradlew test --tests "com.rehearse.api.domain.interview.*"

# 특정 클래스만
./gradlew test --tests "InterviewServiceTest"
```

---

## 테스트 작성 흐름

```
1. 대상 클래스의 public 메서드 목록 확인
2. 메서드별 시나리오 도출 (정상 + 예외 + 경계값)
3. 테스트 유형 결정 (위 규칙 참조)
4. 기존 테스트 패턴 확인 (같은 도메인의 다른 테스트 참고)
5. 테스트 코드 작성 (컨벤션 준수)
6. 실행 및 통과 확인
7. 안티패턴 체크리스트 검토
```

---

## 테스트 Fixture 팩토리

### 목적

여러 테스트에서 반복 생성하는 도메인 엔티티를 공통 팩토리로 제공하여 중복 제거 및 유지보수성 확보.

### 위치

```
src/test/java/com/rehearse/api/global/support/TestFixtures.java
```

### 대상 엔티티

| 팩토리 메서드 | 사용처 (Plan) |
|-------------|-------------|
| `createInterview(...)` | Plan 08, 09 (TransactionHandler, Pipeline) |
| `createQuestionPool(...)` | Plan 05, 06, 07 (QuestionPool 전체) |
| `createQuestionSet(...)` | Plan 09 (Pipeline) |
| `createReviewBookmark(...)` | Plan 10 (ReviewBookmarkFinder) |

### 설계 규칙

- **정적 팩토리 메서드 패턴**: `TestFixtures.createInterview()` 형태
- **합리적 기본값 제공**: 필수 파라미터만 받고 나머지는 기본값 사용
- **오버로드로 유연성 확보**: 특정 필드를 지정해야 하는 경우 파라미터 추가 오버로드
- **빌더 패턴 지양**: 간결한 팩토리 메서드 선호 (테스트 코드 가독성 우선)
- **Fixture 내에서 Mock 사용 금지**: 실제 엔티티/VO만 생성

```java
// 사용 예시
public class TestFixtures {
    public static Interview createInterview() {
        return Interview.create(1L, Position.BACKEND, InterviewLevel.JUNIOR, ...);
    }

    public static Interview createInterview(Long userId, Position position) {
        return Interview.create(userId, position, InterviewLevel.JUNIOR, ...);
    }

    public static QuestionPool createQuestionPool(String cacheKey, String content) {
        return QuestionPool.create(cacheKey, content, null, null, null, null);
    }
}
```

### 안티패턴

- Fixture 클래스에 비즈니스 로직 포함 금지
- Fixture에서 Repository/Service 호출 금지 (순수 객체 생성만)
- 모든 엔티티에 Fixture 만들지 않기 — **2개 이상 테스트 파일에서 사용될 때만** 추가

---

## 기존 테스트 보강 우선순위

신규 테스트가 필요한 클래스를 우선순위 순으로 정리한다. 이 목록은 커버리지 갭 분석 시 업데이트한다.

### Phase 1: QuestionPool 도메인 (테스트 0%)
캐싱 전략, 동시성 제어, 키워드 매칭 — 회귀 시 전체 질문 생성에 영향

### Phase 2: Interview 질문 생성 파이프라인
`QuestionGenerationService`, `QuestionGenerationTransactionHandler`, `QuestionGenerationEventHandler`

### Phase 3: TTS / Admin 컨트롤러
신규 기능, HTTP 계약 미검증

### Phase 4: Infra AI 클라이언트
`ClaudeApiClient`, `ResilientAiClient`, `AiResponseParser` — 외부 장애 대응

### Phase 5: Global/Security 보강
`GlobalExceptionHandler`, `GlobalRateLimiterFilter`, `CustomOAuth2UserService`
