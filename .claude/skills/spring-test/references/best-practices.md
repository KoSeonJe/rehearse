# 테스트 베스트 프랙티스

> Java 21 + Spring Boot 3.x 환경에서의 테스트 베스트 프랙티스.
> 2025-2026 기준 최신 트렌드와 실전 패턴.

---

## 1. 테스트 피라미드

```
         ╱╲
        ╱  ╲       Integration (10%)
       ╱────╲      @SpringBootTest — 전체 플로우
      ╱      ╲
     ╱  Slice  ╲   Slice (20%)
    ╱    Tests   ╲  @WebMvcTest, @DataJpaTest — 레이어별
   ╱──────────────╲
  ╱   Unit Tests   ╲  Unit (70%)
 ╱  Fast & Isolated  ╲ @ExtendWith(MockitoExtension.class), 순수 Java
╱────────────────────────╲
```

**원칙**: 가장 가벼운 테스트 유형으로 충분히 검증할 수 있으면 그것을 사용한다.

---

## 2. 독립성 (Test Isolation)

**각 테스트는 다른 테스트에 의존하지 않는다.**

```java
// BAD: 테스트 간 상태 공유
static User sharedUser;

@Test void test1_create() { sharedUser = service.create(...); }
@Test void test2_update() { service.update(sharedUser, ...); }  // test1에 의존

// GOOD: 각 테스트가 독립적으로 데이터 준비
@Test void create_success() {
    User user = service.create(createMockRequest());
    assertThat(user).isNotNull();
}

@Test void update_success() {
    User user = createMockUser();  // 독립적으로 준비
    service.update(user, updateRequest);
    assertThat(user.getName()).isEqualTo("updated");
}
```

---

## 3. 하나의 관심사만 검증

```java
// BAD: 여러 관심사를 한 테스트에서 검증
@Test void createInterview_test() {
    // 생성 검증 + 저장 검증 + 알림 검증 + 로그 검증...
}

// GOOD: 관심사별 분리
@Test void createInterview_success_returnsResponse() { ... }
@Test void createInterview_success_savesToRepository() { ... }
@Test void createInterview_success_sendsNotification() { ... }
```

---

## 4. 행위 검증 vs 구현 검증

**구현 세부사항이 아닌 행위(결과)를 검증한다.**

```java
// BAD: 내부 구현에 의존 (리팩토링 시 깨짐)
@Test void createInterview() {
    service.createInterview(request, null);
    verify(repository).save(any());          // 어떤 메서드를 호출했는지 확인 → 구현 의존
    verify(mapper).toEntity(any());          // 매퍼 호출 확인 → 구현 의존
    verify(validator).validate(any());       // 검증기 호출 확인 → 구현 의존
}

// GOOD: 결과(행위)를 검증
@Test void createInterview_success_returnsCreatedInterview() {
    given(aiClient.generateQuestions(any())).willReturn(mockQuestions);

    InterviewResponse response = service.createInterview(request, null);

    assertThat(response.getPosition()).isEqualTo(Position.BACKEND);
    assertThat(response.getStatus()).isEqualTo(InterviewStatus.READY);
}
```

**예외**: 부수 효과(side effect)가 핵심 비즈니스 요구사항인 경우는 `then().should()` 사용.
예: "면접 생성 시 저장이 반드시 발생해야 한다" → `then(repository).should().save(any())`

---

## 5. @Nested로 구조화

**테스트 케이스가 5개 이상이면 @Nested로 메서드/시나리오별 그룹화한다.**

```java
@DisplayName("InterviewService")
@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Nested
    @DisplayName("createInterview")
    class CreateInterview {
        @Test
        @DisplayName("정상 입력으로 면접을 생성한다")
        void withValidRequest_returnsResponse() { ... }

        @Test
        @DisplayName("이력서 없이 기본 질문으로 면접을 생성한다")
        void withoutResume_usesDefaultQuestions() { ... }

        @Test
        @DisplayName("AI 응답 실패 시 예외를 던진다")
        void aiFailure_throwsException() { ... }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {
        @Test
        @DisplayName("READY에서 IN_PROGRESS로 전이한다")
        void readyToInProgress_statusChanges() { ... }

        @Test
        @DisplayName("잘못된 상태 전이 시 예외가 발생한다")
        void invalidTransition_throwsException() { ... }
    }
}
```

---

## 6. @ParameterizedTest 활용

**동일 로직에 대해 여러 입력을 테스트할 때 사용한다.**

```java
@ParameterizedTest
@DisplayName("유효하지 않은 duration 값이면 검증 실패")
@ValueSource(ints = {0, -1, -100, 181})
void createInterview_invalidDuration_throwsException(int duration) {
    CreateInterviewRequest request = createRequestWithDuration(duration);
    assertThatThrownBy(() -> service.createInterview(request, null))
            .isInstanceOf(BusinessException.class);
}

@ParameterizedTest
@DisplayName("상태 전이 규칙 검증")
@CsvSource({
    "READY, IN_PROGRESS, true",
    "IN_PROGRESS, COMPLETED, true",
    "READY, COMPLETED, false",
    "COMPLETED, READY, false"
})
void updateStatus_transitionRules(InterviewStatus from, InterviewStatus to, boolean valid) {
    Interview interview = createInterviewWithStatus(from);
    if (valid) {
        assertThatCode(() -> interview.updateStatus(to)).doesNotThrowAnyException();
    } else {
        assertThatThrownBy(() -> interview.updateStatus(to))
                .isInstanceOf(BusinessException.class);
    }
}

@ParameterizedTest
@DisplayName("직무별 질문 개수 계산")
@MethodSource("providePositionAndExpectedCount")
void calculateQuestionCount_byPosition(Position position, int expected) {
    assertThat(calculator.calculate(position)).isEqualTo(expected);
}

static Stream<Arguments> providePositionAndExpectedCount() {
    return Stream.of(
        Arguments.of(Position.BACKEND, 8),
        Arguments.of(Position.FRONTEND, 7),
        Arguments.of(Position.FULLSTACK, 10)
    );
}
```

---

## 7. 예외 테스트 패턴

```java
// 기본: assertThatThrownBy
@Test
@DisplayName("존재하지 않는 면접 ID로 조회하면 NOT_FOUND 예외")
void findById_notExists_throwsNotFoundException() {
    given(interviewRepository.findById(999L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> interviewFinder.findById(999L))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException bex = (BusinessException) ex;
                assertThat(bex.getErrorCode()).isEqualTo(InterviewErrorCode.NOT_FOUND);
            });
}

// 예외가 발생하지 않아야 하는 경우
@Test
@DisplayName("유효한 입력이면 예외 없이 처리된다")
void validInput_noException() {
    assertThatCode(() -> service.process(validRequest))
            .doesNotThrowAnyException();
}
```

---

## 8. Controller (Slice) 테스트 패턴

```java
@WebMvcTest(InterviewController.class)
@Import(TestSecurityConfig.class)
class InterviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private InterviewService interviewService;

    @Test
    @WithMockUserId(userId = 1L)
    @DisplayName("면접 생성 API가 201을 반환한다")
    void createInterview_returns201() throws Exception {
        // given
        given(interviewService.createInterview(any(), any()))
                .willReturn(createMockResponse());

        // when & then
        mockMvc.perform(post("/api/v1/interviews")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createMockRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.position").value("BACKEND"));
    }

    @Test
    @DisplayName("인증 없이 접근하면 401을 반환한다")
    void withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/interviews"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUserId(userId = 1L)
    @DisplayName("유효하지 않은 요청이면 400을 반환한다")
    void invalidRequest_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/interviews")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))  // 필수값 누락
                .andExpect(status().isBadRequest());
    }
}
```

---

## 9. Repository (Slice) 테스트 패턴

```java
@DataJpaTest
@ActiveProfiles("test")
class InterviewRepositoryTest {

    @Autowired private InterviewRepository interviewRepository;
    @Autowired private TestEntityManager em;

    @Test
    @DisplayName("Fetch Join으로 질문과 함께 면접을 조회한다")
    void findByIdWithQuestions_returnsWithQuestions() {
        // given
        Interview interview = createAndPersistInterview();
        em.flush();
        em.clear();  // 1차 캐시 초기화 (실제 쿼리 발생 보장)

        // when
        Interview found = interviewRepository.findByIdWithQuestions(interview.getId())
                .orElseThrow();

        // then
        assertThat(found.getQuestions()).hasSize(3);
        // LazyInitializationException이 발생하지 않아야 한다
    }

    @Test
    @DisplayName("사용자 ID로 면접 목록을 최신순으로 조회한다")
    void findByUserIdOrderByCreatedAtDesc_returnsSorted() {
        // given
        User user = createAndPersistUser();
        createAndPersistInterviews(user, 3);
        em.flush();

        // when
        List<Interview> interviews = interviewRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId());

        // then
        assertThat(interviews)
                .hasSize(3)
                .isSortedAccordingTo(Comparator.comparing(
                        Interview::getCreatedAt).reversed());
    }
}
```

---

## 10. Entity (Unit) 테스트 패턴

```java
class InterviewTest {

    @Test
    @DisplayName("면접 생성 시 기본 상태는 READY이다")
    void create_defaultStatus_isReady() {
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .interviewType(InterviewType.TECHNICAL)
                .build();

        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.READY);
    }

    @Test
    @DisplayName("techStack이 null이면 기본값을 반환한다")
    void getEffectiveTechStack_null_returnsDefault() {
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .build();

        assertThat(interview.getEffectiveTechStack()).isNotEmpty();
    }

    @Nested
    @DisplayName("상태 전이")
    class StatusTransition {
        @Test
        @DisplayName("READY → IN_PROGRESS 전이 성공")
        void readyToInProgress_success() {
            Interview interview = createReadyInterview();
            interview.updateStatus(InterviewStatus.IN_PROGRESS);
            assertThat(interview.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("COMPLETED → READY 전이 실패")
        void completedToReady_throwsException() {
            Interview interview = createCompletedInterview();
            assertThatThrownBy(() -> interview.updateStatus(InterviewStatus.READY))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
```

---

## 11. Soft Assertions (다중 검증)

**하나의 객체에 대해 여러 필드를 검증할 때 SoftAssertions로 한 번에 실패 리포트.**

```java
@Test
@DisplayName("면접 응답 DTO가 모든 필드를 올바르게 매핑한다")
void from_mapsAllFields() {
    Interview interview = createMockInterview();

    InterviewResponse response = InterviewResponse.from(interview);

    SoftAssertions.assertSoftly(softly -> {
        softly.assertThat(response.getId()).isEqualTo(interview.getId());
        softly.assertThat(response.getPosition()).isEqualTo(interview.getPosition());
        softly.assertThat(response.getStatus()).isEqualTo(interview.getStatus());
        softly.assertThat(response.getDurationMinutes()).isEqualTo(interview.getDurationMinutes());
    });
}
```

---

## 12. Mock 사용 원칙

### Mock 대상 (외부 의존성)

- Repository (Unit Test에서)
- 외부 API 클라이언트 (AiClient, S3Client 등)
- Finder (타 도메인 접근 시)
- 시간/랜덤 같은 비결정적 요소

### Mock 금지 (Real 사용)

- Entity 생성 및 도메인 로직
- DTO 변환 (`Response.from(entity)`)
- 유틸리티 메서드
- 값 객체 (VO)

### Over-Mocking 징후

- Mock 설정이 테스트 로직보다 긴 경우
- `verify()`가 3개 이상인 경우
- given() 체인이 5줄 이상인 경우

→ 테스트 대상 클래스의 책임이 과도하거나, 테스트 유형이 부적절한 신호.
