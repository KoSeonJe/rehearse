# 프로젝트 테스트 컨벤션

> 이 프로젝트(Rehearse)에서 확립된 테스트 작성 규칙.
> 새 테스트 작성 시 반드시 이 컨벤션을 따른다.

---

## 1. 테스트 어노테이션

| 테스트 유형 | 어노테이션 | 사용처 |
|-----------|----------|-------|
| Unit (Service) | `@ExtendWith(MockitoExtension.class)` | Service, Finder, 유틸 |
| Unit (Entity) | 없음 (순수 JUnit5) | Entity 도메인 로직 |
| Slice (Controller) | `@WebMvcTest(XxxController.class)` | HTTP 계층 |
| Slice (Repository) | `@DataJpaTest` | JPA 쿼리, Fetch 전략 |
| Integration | `@SpringBootTest` + `@ActiveProfiles("test")` | 전체 플로우 (최소한) |

## 2. 메서드 네이밍

**패턴**: `methodName_scenario_expectedResult`

```java
// Good
void createInterview_withValidRequest_returnsResponse()
void createInterview_withoutResume_usesDefaultQuestions()
void updateStatus_readyToInProgress_statusChanges()
void findById_notExists_throwsNotFoundException()

// Bad
void testCreateInterview()          // 'test' 접두사 금지
void create()                       // 시나리오 불명확
void shouldCreateInterviewSuccessfully()  // 다른 프로젝트 패턴
```

## 3. @DisplayName

**한국어로 테스트 의도를 서술한다.** 메서드명으로는 표현이 제한되는 비즈니스 맥락을 여기에 쓴다.

```java
@Test
@DisplayName("면접 세션 생성 시 Claude API로 질문을 생성하고 저장한다")
void createInterview_success() { ... }

@Test
@DisplayName("이미 진행 중인 면접의 상태를 변경하면 예외가 발생한다")
void updateStatus_alreadyInProgress_throwsException() { ... }
```

## 4. BDDMockito 스타일

**`given` / `willReturn` / `then().should()` 패턴을 사용한다.** Mockito의 `when`/`verify` 대신.

```java
// Given (스텁 설정)
given(interviewFinder.findById(1L)).willReturn(mockInterview);
given(aiClient.generateQuestions(any())).willReturn(mockQuestions);

// When (실행)
InterviewResponse response = interviewService.createInterview(request, null);

// Then (검증)
assertThat(response.getId()).isEqualTo(1L);
then(interviewRepository).should().save(any(Interview.class));
then(aiClient).should(never()).retryGeneration(any());
```

## 5. AssertJ 검증

**모든 검증에 AssertJ fluent API를 사용한다.** JUnit의 `assertEquals` 금지.

```java
// 기본 검증
assertThat(result).isNotNull();
assertThat(result.getId()).isEqualTo(1L);
assertThat(result.getStatus()).isEqualTo(InterviewStatus.READY);

// 컬렉션 검증
assertThat(questions)
    .hasSize(5)
    .extracting(Question::getType)
    .contains(QuestionType.TECHNICAL, QuestionType.BEHAVIORAL);

// 예외 검증
assertThatThrownBy(() -> service.updateStatus(id, newStatus))
    .isInstanceOf(BusinessException.class)
    .satisfies(ex -> {
        BusinessException bex = (BusinessException) ex;
        assertThat(bex.getErrorCode()).isEqualTo(InterviewErrorCode.INVALID_STATUS_TRANSITION);
    });
```

## 6. Given-When-Then 주석

**섹션 구분 주석을 반드시 작성한다.**

```java
@Test
@DisplayName("면접 세션을 생성한다")
void createInterview_success() {
    // given
    CreateInterviewRequest request = createMockRequest();
    given(aiClient.generateQuestions(any())).willReturn(mockQuestions);

    // when
    InterviewResponse response = interviewService.createInterview(request, null);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getPosition()).isEqualTo(Position.BACKEND);
}
```

## 7. Mock 관련

### @MockitoBean (Spring 3.4+)

```java
// Slice Test에서 사용
@WebMvcTest(InterviewController.class)
class InterviewControllerTest {
    @MockitoBean InterviewService interviewService;  // @MockBean 대신
    @Autowired MockMvc mockMvc;
}
```

### @Mock + @InjectMocks (Unit Test)

```java
@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {
    @InjectMocks private InterviewService interviewService;
    @Mock private InterviewRepository interviewRepository;
    @Mock private InterviewFinder interviewFinder;
    @Mock private AiClient aiClient;
}
```

## 8. 헬퍼 메서드

**반복되는 Mock 객체 생성은 `createMock...()` 패턴으로 추출한다.**

```java
private Interview createMockInterview() {
    Interview interview = Interview.builder()
            .position(Position.BACKEND)
            .interviewType(InterviewType.TECHNICAL)
            .durationMinutes(30)
            .build();
    ReflectionTestUtils.setField(interview, "id", 1L);
    return interview;
}

private CreateInterviewRequest createMockRequest() {
    // Request 객체 생성
}
```

## 9. Security 테스트

**`@WithMockUserId` 커스텀 어노테이션을 사용한다.**

```java
@Test
@WithMockUserId(userId = 1L)
@DisplayName("인증된 사용자가 면접 목록을 조회한다")
void getInterviews_authenticated_returnsList() throws Exception {
    mockMvc.perform(get("/api/v1/interviews")
                    .with(csrf()))
            .andExpect(status().isOk());
}
```

## 10. 디렉토리 구조

**소스 코드와 동일한 패키지 구조를 테스트에서도 유지한다.**

```
src/test/java/com/rehearse/api/
├── domain/
│   ├── interview/
│   │   ├── controller/InterviewControllerTest.java
│   │   ├── entity/InterviewTest.java
│   │   ├── repository/InterviewRepositoryTest.java
│   │   └── service/InterviewServiceTest.java
│   ├── auth/
│   │   └── controller/AuthControllerTest.java
│   └── ...
├── global/
│   └── config/TestSecurityConfig.java
├── infra/
│   └── ai/...
└── support/              ← 테스트 유틸리티
    ├── WithMockUserId.java
    └── WithMockUserIdSecurityContextFactory.java
```

## 11. 테스트 설정

- **프로파일**: `@ActiveProfiles("test")` → `application-test.yml` 로드
- **DB**: H2 인메모리 (`jdbc:h2:mem:rehearse`)
- **JPA**: `ddl-auto: create-drop` (테스트마다 스키마 재생성)
- **Flyway**: 비활성화 (Hibernate가 스키마 관리)
- **외부 서비스**: 모두 Mock 또는 비활성화
