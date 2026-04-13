# Backend 테스트 대규모 개선 — 요구사항 정의

> 상태: Completed
> 작성일: 2026-04-13

## Why

### 1. Why? — 어떤 문제를 해결하는가

1. **테스트 0% 도메인 존재**: QuestionPool(캐싱/동시성/매칭), TTS, Admin이 전혀 테스트되지 않아 변경 시 회귀 감지 불가
2. **핵심 파이프라인 미테스트**: Interview 질문 생성 파이프라인(`QuestionGenerationService` → `EventHandler` → `TransactionHandler`)이 테스트 없이 운영 중
3. **컨벤션 불일치**: 기존 테스트의 56%가 Given-When-Then 주석 누락, 100%가 @Nested 미사용으로 가독성/유지보수성 저하
4. **외부 장애 대응 미검증**: `ClaudeApiClient`, `ResilientAiClient`의 재시도/폴백 로직이 테스트 없음

### 2. Goal — 구체적인 결과물과 성공 기준

- 기존 46개 테스트 파일 전체 컨벤션 통일 (GWT 주석, @Nested, @DisplayName, BDDMockito)
- 21개 신규 테스트 파일, ~210-240개 신규 테스트 메서드 추가
- 클래스 기준 테스트 커버율 42% → 85%+
- `./gradlew test` 전체 통과 유지

### 3. Evidence — 근거

- 커버리지 갭 분석 결과: QuestionPool 0%, TTS 0%, Admin 0%, Interview 파이프라인 67%
- 컨벤션 감사 결과: GWT 주석 44% 준수, @Nested 0% 사용, @DisplayName 93% 준수
- `backend/TEST_STRATEGY.md`에 정의된 우선순위 및 테스트 유형 선택 규칙

### 4. Trade-offs — 포기하는 것

- **Integration Test 최소화** — `@SpringBootTest` 통합 테스트는 이 이터레이션에서 추가하지 않음. Unit + Slice로 충분히 검증. 단, 차기 이터레이션에서 핵심 파이프라인 Integration Test 추가 예정 (아래 "차기 이터레이션" 섹션 참조)
- **프로덕션 코드 변경 없음** — 테스트 용이성을 위한 리팩토링은 별도 이슈로 분리
- **100% 커버리지 미추구** — 단순 DTO/Enum/Config는 테스트하지 않음 (TEST_STRATEGY.md 기준)
- **기존 테스트 케이스 보강 미포함** — 메서드 수가 부족한 기존 테스트(InterviewCreationServiceTest 1개, InterviewQueryServiceTest 2개 등)의 케이스 추가는 이번 스코프 외. 구조 개선(GWT, @Nested)만 수행

---

## 목표

세 개의 Workstream으로 진행:

- **Workstream A (Plan 01-04)**: 기존 테스트 파일 컨벤션 리팩토링 — 로직 변경 없이 구조만 개선
  - Plan 04 수정: 5개 미만 메서드 파일(6개)은 @Nested 제외 — 그룹화 대상 부족
  - Plan 03 수정: PersonaResolverTest(실측 9개 메서드)를 Plan 04로 이동
- **Workstream B-0 (사전 작업)**: 테스트 Fixture 팩토리 클래스 생성 — Workstream B에서 공통 사용
- **Workstream B (Plan 05-15)**: 21개 미테스트 클래스에 대한 신규 테스트 작성

## Scope

- **In**: 백엔드 `src/test/` 하위 모든 테스트 파일 컨벤션 통일, `TEST_STRATEGY.md` Phase 1-5 신규 테스트
- **Out**: 프로덕션 코드 변경, `@SpringBootTest` 통합 테스트 추가, 프론트엔드 테스트, DTO/Enum/Config 단순 클래스 테스트

## 제약조건

- 프로덕션 코드(`src/main/`) 변경 0건
- Workstream A 완료 후 Workstream B 시작 (컨벤션 확립 → 신규 테스트 적용)
- 각 Plan은 1개 PR로 관리 (BE 스코프, 리뷰 가능한 크기)
- 모든 테스트는 `backend/TEST_STRATEGY.md` 및 `.claude/skills/spring-test/references/` 컨벤션 준수

---

## 의존성 그래프

```
WORKSTREAM A: 컨벤션 리팩토링 (all parallel)
═════════════════════════════════════════

  Plan 01 (GWT 주석, 24파일)      ─┐
  Plan 02 (@DisplayName, 3파일)   ─┤
  Plan 03 (@Nested 10+, 12파일)   ─┼─▶  GATE: 컨벤션 확립 완료
  Plan 04 (@Nested 5-9, 11파일)   ─┘
                                        │
WORKSTREAM B-0: 사전 작업               ▼
═════════════════════════════════════════
  TestFixtures 팩토리 클래스 생성  ─────▶  GATE: Fixture 준비 완료
                                        │
WORKSTREAM B: 신규 테스트 작성          ▼
═════════════════════════════════════════

Wave 2 (parallel):
  Plan 05 (Entity/VO/Util)  Plan 08 (Distribution+TxH)  Plan 10 (Controllers)
  Plan 12 (AI Parser)       Plan 11 (AdminFB)            Plan 15 (Global)

Wave 3:
  Plan 06 (PoolService)  ◀── Plan 05
  Plan 13 (Claude)       ◀── Plan 12

Wave 4:
  Plan 07 (Providers)    ◀── Plan 06
  Plan 14 (Resilient)    ◀── Plan 13

Wave 5:
  Plan 09 (Pipeline)     ◀── Plan 07 + Plan 08
```

## 성공 기준

1. 기존 테스트 파일 — GWT 주석(24파일), @Nested(5+ 메서드 파일만, 23파일), @DisplayName(3파일 보완), BDDMockito 준수
2. 신규 21개 테스트 파일, ~210-240개 테스트 메서드 추가
3. `TestFixtures` 팩토리 클래스 생성 및 신규 테스트에서 활용
4. `./gradlew test` 전체 통과
5. `lenient().when()` 0건
6. 프로덕션 코드 변경 0건

---

## 차기 이터레이션 (Integration Test)

이번 이터레이션에서 의도적으로 제외한 `@SpringBootTest` 통합 테스트를 차기에 추가한다.

### 대상

| 대상 | 이유 | 우선순위 |
|------|------|:---:|
| 질문 생성 파이프라인 (`QGenService → TxHandler → EventHandler`) | 핵심 플로우, VirtualThread 비동기 동작은 Unit mock으로 검증 불가 | P0 |
| `ResilientAiClient` `@PostConstruct` init() | nullable 생성자 조합이 Spring context에서 정상 동작하는지 검증 | P1 |
| AdminFeedbackController + DB 페이지네이션 | Pageable 실제 DB 바인딩 검증 | P2 |

### 비고

- 현재 Unit **86%** / Slice **14%** / Integration **0%** → 차기에서 Integration 10% 목표로 비율 정상화
- `@SpringBootTest` 사용 시 테스트 실행 속도 영향 최소화를 위해 `@DirtiesContext` 지양, 프로파일 분리 적용
