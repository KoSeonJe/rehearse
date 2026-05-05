# 테스트 유형별 상세 가이드

> 각 테스트 유형의 목적, 사용 시점, 템플릿.

---

## Unit Test (단위 테스트)

### 목적
Spring 컨텍스트 없이 순수 Java로 비즈니스 로직을 검증한다.

### 사용 시점
- Service 비즈니스 로직
- Entity 도메인 행위 (상태 전이, 불변식, 계산)
- Finder 조회 + 예외 변환
- DTO 변환 로직
- 유틸리티/헬퍼 클래스

### 템플릿: Service

```java
@ExtendWith(MockitoExtension.class)
class {ClassName}Test {

    @InjectMocks
    private {ClassName} {fieldName};

    @Mock
    private {DependencyType} {dependencyName};
    // ... 추가 Mock

    @Test
    @DisplayName("{한국어 설명}")
    void {methodName}_{scenario}_{expectedResult}() {
        // given
        {given 설정}

        // when
        {실행}

        // then
        {검증}
    }

    // 헬퍼 메서드
    private {Type} createMock{Name}() {
        return {Type}.builder()
                .{field}({value})
                .build();
    }
}
```

### 템플릿: Entity

```java
class {EntityName}Test {

    @Test
    @DisplayName("{한국어 설명}")
    void {methodName}_{scenario}_{expectedResult}() {
        // given
        {Entity} entity = {Entity}.builder()
                .{field}({value})
                .build();

        // when
        {실행}

        // then
        {검증}
    }
}
```

### 성능
- 실행 시간: ~1ms per test
- Spring 컨텍스트: 불필요
- DB: 불필요

---

## Slice Test (슬라이스 테스트)

### 목적
특정 레이어만 로드하여 해당 레이어의 동작을 검증한다.

### @WebMvcTest (Controller)

**검증 대상:**
- HTTP 요청/응답 매핑
- 입력 유효성 검사 (@Valid)
- 인증/인가 동작
- 응답 JSON 구조 (ApiResponse 래핑)
- HTTP 상태 코드

**로드되는 것:** Controller, ControllerAdvice, Filter, Converter
**로드 안 되는 것:** Service, Repository, Entity

```java
@WebMvcTest({ControllerClass}.class)
@Import(TestSecurityConfig.class)
class {ControllerClass}Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private {ServiceClass} {serviceName};

    @Test
    @WithMockUserId(userId = 1L)
    @DisplayName("{한국어 설명}")
    void {endpoint}_{scenario}_{expectedStatus}() throws Exception {
        // given
        given({serviceName}.{method}(any())).willReturn({mockResponse});

        // when & then
        mockMvc.perform({httpMethod}("/api/v1/{resource}")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString({request})))
                .andExpect(status().{expectedStatus}())
                .andExpect(jsonPath("$.data.{field}").value({expected}));
    }
}
```

### @DataJpaTest (Repository)

**검증 대상:**
- JPQL/Native 쿼리 정확성
- Fetch Join / EntityGraph 동작
- LazyInitialization 방지 확인
- 정렬/페이징 동작

**로드되는 것:** JPA 관련 빈, Repository, TestEntityManager
**로드 안 되는 것:** Controller, Service

```java
@DataJpaTest
@ActiveProfiles("test")
class {RepositoryClass}Test {

    @Autowired
    private {RepositoryClass} {repositoryName};

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("{한국어 설명}")
    void {queryMethod}_{scenario}_{expectedResult}() {
        // given
        {Entity} entity = {Entity}.builder().{...}.build();
        em.persistAndFlush(entity);
        em.clear();  // 1차 캐시 초기화

        // when
        {result} = {repositoryName}.{queryMethod}({params});

        // then
        {검증}
    }
}
```

### 성능
- @WebMvcTest: ~200-300ms 컨텍스트 로딩
- @DataJpaTest: ~150-250ms 컨텍스트 로딩
- 캐시: 동일 설정이면 컨텍스트 재사용

---

## Integration Test (통합 테스트)

### 목적
전체 Spring 컨텍스트를 로드하여 레이어 간 상호작용을 검증한다.

### 사용 시점 (최소한만)
- Service → Repository → DB 전체 플로우
- 트랜잭션 전파 동작
- 여러 Service가 협력하는 복합 비즈니스 로직
- 실제 DB 제약조건 검증 (Unique, FK 등)

### 템플릿

```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional  // 테스트 후 자동 롤백
class {ClassName}IntegrationTest {

    @Autowired
    private {ServiceClass} {serviceName};

    @Autowired
    private {RepositoryClass} {repositoryName};

    @Test
    @DisplayName("{한국어 설명}")
    void {scenario}_fullFlow() {
        // given
        {데이터 준비}

        // when
        {Service 호출}

        // then
        {DB 상태 검증}
    }
}
```

### 성능
- 실행 시간: ~1000-2000ms 컨텍스트 로딩 (캐시 시 재사용)
- DB: H2 인메모리
- 주의: `@DirtiesContext` 남용 금지 (컨텍스트 재로딩)

---

## 테스트 유형 선택 Decision Tree

```
대상 코드가 뭔가?
│
├─ Entity 도메인 로직 → Unit Test (순수 Java)
│
├─ Service 비즈니스 로직
│   ├─ 단일 Service 내 로직 → Unit Test (Mockito)
│   └─ Service + DB 연동 필수 → Integration Test
│
├─ Controller HTTP 처리
│   └─ → Slice Test (@WebMvcTest)
│
├─ Repository 쿼리
│   └─ → Slice Test (@DataJpaTest)
│
├─ DTO 변환
│   └─ → Unit Test (순수 Java)
│
└─ 전체 플로우 (레이어 간 상호작용)
    └─ → Integration Test (@SpringBootTest)
```
