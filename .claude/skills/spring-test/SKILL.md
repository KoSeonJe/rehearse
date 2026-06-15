---
name: spring-test
description: "Java 21 + Spring Boot 3.x 테스트 코드 작성 스킬. 프로젝트 컨벤션과 베스트 프랙티스에 맞춘 테스트 생성, 커버리지 분석, 테스트 품질 개선을 수행한다. 사용자가 '테스트', 'test', '테스트 작성', '커버리지', 'coverage', '테스트 추가', 'test 추가', '단위 테스트', 'unit test', '통합 테스트', 'integration test', '슬라이스 테스트', 'slice test' 등을 언급하면 이 스킬을 사용한다."
triggers:
  - 테스트
  - test
  - 테스트 작성
  - 커버리지
  - coverage
  - 테스트 추가
  - test 추가
  - 단위 테스트
  - unit test
  - 통합 테스트
  - integration test
  - 슬라이스 테스트
  - slice test
  - 테스트 코드
  - test code
  - 테스트 전략
  - test strategy
argument-hint: "<target: class, package, domain, or 'all'> [--type=unit|slice|integration|all] [--analyze-only] [--fix-only]"
---

# Java Spring 테스트 작성 스킬

## Purpose

Java 21 + Spring Boot 3.x 백엔드 코드에 대해 **프로젝트 컨벤션 준수 + 베스트 프랙티스 기반**의 테스트를 작성한다.

테스트는 "커버리지 숫자를 올리는 것"이 아니라 **회귀 버그를 방지하고, 리팩토링 안전망을 제공하며, 코드의 의도를 문서화하는 것**이다. 이 스킬은 그 목적에 집중한다.

---

## Workflow

### Phase 0: 범위 선택 및 분석

사용자가 지정한 범위의 소스 코드와 기존 테스트를 분석한다.

**범위 옵션:**

| 범위 | 예시 | 설명 |
|------|------|------|
| 클래스 단위 | `InterviewService` | 특정 클래스 테스트 작성 |
| 패키지 단위 | `domain.interview` | 도메인 패키지 전체 |
| 레이어 단위 | `--type=service` | 모든 Service 클래스 |
| 전체 | `all` | 프로젝트 전체 커버리지 분석 후 우선순위 제시 |

**분석 절차:**

1. **소스 코드 읽기**: 대상 클래스의 public 메서드, 분기 로직, 예외 경로 파악
2. **기존 테스트 확인**: 이미 작성된 테스트가 있으면 읽고 누락된 케이스 파악
3. **의존성 파악**: Mock 대상 (외부 의존성) vs Real 대상 (내부 로직) 구분
4. **커버리지 갭 리포트 출력** (`all` 범위 시)

```markdown
## 커버리지 갭 리포트

| 도메인 | 소스 클래스 | 테스트 파일 | 커버리지 추정 | 우선순위 |
|--------|-----------|-----------|-------------|---------|
| interview | 40+ | 10 | ~80% | LOW |
| auth | 12+ | 1 | ~30% | HIGH |
| file | 8+ | 2 | ~40% | HIGH |
| servicefeedback | 8+ | 2 | ~40% | MEDIUM |

### 권장 작업 순서
1. auth 도메인 — 보안 관련, 회귀 위험 높음
2. file 도메인 — 외부 연동(S3), 엣지 케이스 다수
3. ...
```

---

### Phase 1: 테스트 계획 수립

대상별로 어떤 테스트를 작성할지 계획한다. **계획을 먼저 출력하고 사용자 확인 후 작성한다.**

```markdown
## 테스트 계획: {ClassName}

### 테스트 유형: {Unit | Slice | Integration}
선택 이유: {왜 이 유형인지}

### 테스트 케이스 목록

| # | 메서드 | 시나리오 | 기대 결과 | 우선순위 |
|---|--------|---------|----------|---------|
| 1 | createInterview | 정상 입력 | 면접 세션 생성 | HIGH |
| 2 | createInterview | 이력서 없이 | 기본 질문 생성 | HIGH |
| 3 | createInterview | AI 응답 실패 | BusinessException | MEDIUM |
| 4 | getInterview | 존재하지 않는 ID | NOT_FOUND 예외 | HIGH |

### Mock 대상
- `InterviewRepository` → Mock
- `InterviewFinder` → Mock
- `AiClient` → Mock

### 실제 사용 대상
- Entity 생성/상태 전이 로직
```

---

### Phase 2: 테스트 코드 작성

계획이 승인되면 프로젝트 컨벤션에 맞춰 테스트를 작성한다.

**반드시 준수할 프로젝트 컨벤션:**

참조: `references/project-conventions.md`

1. **테스트 어노테이션**: 레이어별 적합한 어노테이션 사용
2. **메서드 네이밍**: `methodName_scenario_expectedResult`
3. **@DisplayName**: 한국어로 테스트 의도 설명
4. **BDDMockito**: `given()` / `willReturn()` / `then().should()` 스타일
5. **AssertJ**: 모든 검증에 AssertJ fluent API 사용
6. **Given-When-Then 주석**: 섹션 구분 주석 필수
7. **@MockitoBean**: Spring 3.4+ 환경에서 `@MockBean` 대신 사용
8. **헬퍼 메서드**: `createMock...()` 패턴으로 반복 객체 생성

**베스트 프랙티스 체크리스트:**

참조: `references/best-practices.md`

작성 시 아래를 모두 확인:
- [ ] 각 테스트는 독립적 (순서 무관, 상태 공유 없음)
- [ ] 하나의 테스트에 하나의 검증 관심사
- [ ] Mock은 외부 의존성만 (Over-mocking 금지)
- [ ] 구현 세부사항이 아닌 행위/결과 검증
- [ ] 예외 경로 테스트 포함
- [ ] 경계값 테스트 포함 (빈 리스트, null, 최대값 등)
- [ ] @Nested로 메서드별/시나리오별 그룹화 (5개+ 케이스 시)
- [ ] @ParameterizedTest 활용 (동일 로직 다중 입력 시)

---

### Phase 3: 테스트 품질 검증

작성된 테스트가 실제로 동작하는지 확인한다.

1. **컴파일 확인**: `./gradlew compileTestJava` 성공
2. **테스트 실행**: `./gradlew test --tests "{TestClassName}"` 통과
3. **실패 시 수정**: 에러 분석 후 즉시 수정
4. **품질 체크**: `references/anti-patterns.md`의 안티패턴 미해당 확인

---

## 테스트 유형별 가이드

### 레이어 → 테스트 유형 매핑

참조: `references/test-types.md`

| 대상 레이어 | 기본 테스트 유형 | 어노테이션 | 핵심 검증 |
|-----------|--------------|----------|----------|
| Entity | Unit | 없음 (순수 Java) | 도메인 로직, 상태 전이, 불변식 |
| Service | Unit | `@ExtendWith(MockitoExtension.class)` | 비즈니스 로직, 오케스트레이션 |
| Finder | Unit | `@ExtendWith(MockitoExtension.class)` | 조회 + 예외 변환 |
| Controller | Slice | `@WebMvcTest` | HTTP 상태, 요청/응답 형식, 검증 |
| Repository | Slice | `@DataJpaTest` | 쿼리 정확성, Fetch 전략 |
| DTO | Unit | 없음 | `from()` 변환 정확성 |
| 전체 플로우 | Integration | `@SpringBootTest` | E2E 흐름 (최소한만) |

---

## Options

- `--type=unit|slice|integration|all`: 작성할 테스트 유형 (기본: 레이어에 맞게 자동 선택)
- `--analyze-only`: 커버리지 갭 분석만 출력, 코드 작성 안 함
- `--fix-only`: 기존 테스트의 컨벤션/품질 문제만 수정
- `--with-nested`: @Nested 클래스로 구조화 (5개+ 케이스 시 자동 적용)
- `--with-parameterized`: @ParameterizedTest 적극 활용

## Examples

```
# 특정 서비스 테스트 작성
/spring-test InterviewService

# 도메인 패키지 전체 테스트
/spring-test domain.interview

# 전체 프로젝트 커버리지 분석만
/spring-test all --analyze-only

# 컨트롤러 슬라이스 테스트만
/spring-test InterviewController --type=slice

# 기존 테스트 품질 개선
/spring-test domain.interview --fix-only

# Entity 단위 테스트
/spring-test Interview --type=unit
```

## Agent 활용

| 작업 | 에이전트 | 역할 |
|------|---------|------|
| 테스트 작성 | `backend` | 테스트 코드 구현 |
| 테스트 전략 | `test-engineer` | 전략 수립, 커버리지 분석 |
| 테스트 리뷰 | `code-reviewer` | 테스트 품질, 안티패턴 검출 |
| 코드 분석 | `general-purpose` | 소스 코드 탐색 |

## Notes

- **테스트 우선순위**: 비즈니스 로직(Service/Entity) > API 계약(Controller) > 데이터 접근(Repository)
- **점진적 작성**: 한 번에 모든 테스트를 작성하지 않는다. 도메인 단위로 진행.
- **기존 패턴 존중**: 프로젝트에 이미 확립된 테스트 패턴을 먼저 파악하고 따른다.
- **ReflectionTestUtils**: Entity ID 설정 등 불가피한 경우에만 사용.
- **한국어**: @DisplayName, 주석, 리포트 모두 한국어.
- **테스트도 코드다**: 중복 제거, 가독성, 유지보수성을 프로덕션 코드와 동일하게 신경 쓴다.
