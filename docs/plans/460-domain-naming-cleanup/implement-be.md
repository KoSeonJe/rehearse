# Implement BE — 도메인 enum/필드/변환 네이밍 충돌 정리 (Phase 1)

> **작성자**: backend agent (Plan-only / 코드 변경 0)
> **선행 승인**: tech-spec.md 사용자 승인 완료
> **분기**: BE+FE 동시 (`implement-be.md` + `implement-fe.md`). 머지 순서 = **BE 선행** → FE 즉시 연속 머지.
> **임계 (`docs/plans/AGENTS.md §6`)**: Task 10개 (≥8) → `tasks/be-NN-*.md` 분리.

---

## Why → Goal (1줄 미러)

같은 의미 → 같은 이름. 동작 변경 0. **BE 단위/통합/E2E 100% 통과 + grep 5종 0건**.

## Evidence (BE 영향 카탈로그 — 직접 검증)

- `interview.Perspective` 임포트 6 파일: `AnswerAnalyzer`, `AnswerAnalyzerPromptBuilder`, `AudioTurnAnalyzerPromptBuilder`, `FollowUpPromptBuilder`, `AskedPerspectives`, `AnswerAnalysisJsonRenderer` (+ 테스트 6 파일).
- `feedback.FeedbackPerspective` 임포트 7 파일: `QuestionType`, `RubricFamily`, `RubricLoader`, `AnswerResponse`, `TimestampFeedbackResponse` (+ 테스트 4 파일 + 리소스 YAML 2개).
- `QuestionSetCategory` 사용 11 파일 (main): `QuestionSet`, `QuestionSetRepository`, `QuestionSetAssembler`, `ResumeQuestionPersister`, `ResumeTurnEventPublisher`, `ResumeInterviewOrchestrator`, `FollowUpTransactionHandler`, `RubricFamily`, `RubricLoader`, `AnswerResponse`, `TimestampFeedbackResponse` (+ 테스트 다수).
- `formatPerspectives` 정의 5곳 / `toReferenceLabel` 정의 3곳 (tech-spec L21~22 좌표 동일).
- `FollowUpExchange` cascade 15 파일 (tech-spec L24~28 좌표 동일). Lombok `.answer()` getter 호출 = `.answerText()` 일괄 갱신.
- `selectedPerspective` 필드/호출 5곳 (tech-spec L30~35 좌표 동일).
- `feedbackPerspective` JSON 키 / 필드 / YAML 키 위치:
  - `AnswerResponse.feedbackPerspective` (line 17, 25, 34) — JSON 키.
  - `TimestampFeedbackResponse` (line 156 매핑, 필드 별도 위치) — JSON 키.
  - `RubricFamily.MappingRule.feedbackPerspective` (line 41, 54-58, 67) — record 컴포넌트.
  - `RubricLoader` (line 71, 169, 180-184) — YAML 키 파싱.
  - `_mapping.yaml` (line 8 주석, 38 키).

### 추가 발견 (tech-spec 미반영)

- `experience-technical-rubric.yaml:9` `- feedbackPerspective: EXPERIENCE` 키 존재. `RubricLoader.parseRubric` 은 해당 키 미파싱 (메타데이터/주석성 — `applies_to:` 블록). **단어 일관성** 차원에서 변경 권장. Task 9 에 포함.
- DB 스키마 변경 0 검증 완료: `question_set.category VARCHAR(50)` 값 = `InterviewType.name()` 12값 100% 일치 (tech-spec Data Model + handoff `DB 안전성` 일치).

## Trade-offs

상위 결정은 tech-spec 본문 (Trade-off 1~4) 단일 출처. implement 단계에서는 **task 순서** 만 trade-off:

### Task 순서 — Top-down vs Bottom-up

#### Option A (채택): Bottom-up (enum rename → DTO/필드 rename → 변환 단일 출처화 → YAML/Rubric → 테스트 일괄)
- 장점: enum rename 후 컴파일 오류로 임포트 누락 즉시 검출. 각 task 단위 독립 컴파일/테스트 GREEN 가능.
- 단점: 초기 task (enum rename) 가 cascade 큼 → 단일 PR 전체 빌드는 마지막 task 까지 RED 가능.
- 사유: IntelliJ Safe Rename 활용 시 cascade 자동 처리. tech-spec L367 `IDE rename refactor 사용 권장` 일치.

#### Option B (폐기): Top-down (DTO 부터 → enum rename 마지막)
- 장점: 외부 API contract 먼저 확정 → FE 병렬 시작 더 빠름.
- 폐기 사유: 본 plan 은 BE+FE 즉시 연속 머지 (사용자 결정) → FE 병렬 시작 이점 작음. enum cascade 가 가장 큰 = 먼저 처리해서 후속 task 안정화.

### Task 단위 = 커밋 단위 (`.claude/rules/commit.md`)

- 1 task = 1 commit 원칙. PR 1개에 10 commit (논리 단위 분리).
- 빌드/테스트 통과 상태로 각 commit 보장 권장하나, enum rename cascade 시 일부 중간 commit 컴파일 RED 가능 → tech-spec L367 IDE Safe Rename 권장 활용 시 1 commit GREEN 가능.
- **타협**: Task 1~3 (enum rename) 각각 IDE Safe Rename 단일 commit 으로 GREEN 유지. Task 4~9 는 작은 변경 → 자연스럽게 GREEN.

## Tasks

| # | 제목 | Implement | Review | 의존 | Verification 핵심 |
|---|------|-----------|--------|------|------------------|
| 1 | `Perspective` → `AnswerFeedbackPerspective` rename | `backend` | `code-reviewer-backend` | — | `grep "import.*interview.entity.Perspective\b"` = 0 |
| 2 | `FeedbackPerspective` → `RubricCategory` rename + 파일 이동 (`feedback/entity/` → `feedback/rubric/entity/`) | `backend` | `code-reviewer-backend` | T1 무관 [parallel] | `grep "import.*feedback.entity.FeedbackPerspective"` = 0 |
| 3 | `QuestionSetCategory` 삭제 + `InterviewType` 통합 | `backend` | `code-reviewer-backend` | T2 (RubricFamily 시그니처 후속) | `grep "QuestionSetCategory" backend/src` = 0 |
| 4 | `PromptFormatters` 신규 + 변환 함수 단일 출처화 | `backend` | `code-reviewer-backend` | T1 (AnswerFeedbackPerspective 시그니처) | `grep "private static String (formatPerspectives\|toReferenceLabel)"` = 0 |
| 5 | `FollowUpExchange.answer` → `answerText` (필드+생성자+getter cascade 15 파일+JSON 키) | `backend` | `code-reviewer-backend` | — | `grep "private String answer\b" FollowUpRequest.java` = 0 |
| 6 | `selectedPerspective` → `selectedAnswerFeedbackPerspective` (FollowUpRequest/Response/Service/GeneratedFollowUp + JSON 키) | `backend` | `code-reviewer-backend` | T1 | `grep "selectedPerspective" backend/src/main/java` = 0 |
| 7 | `AnswerResponse.feedbackPerspective` → `rubricCategory` + `from()` 시그니처 (`InterviewType`) | `backend` | `code-reviewer-backend` | T2, T3 | JSON 응답 키 `rubricCategory` |
| 8 | `TimestampFeedbackResponse$TechnicalFeedback.perspective` → `rubricCategory` (inner 필드 + JSON 키, `technicalFeedback` 객체 내부) | `backend` | `code-reviewer-backend` | T2 [parallel with T7] | JSON 응답 = `technicalFeedback.rubricCategory` |
| 9 | `RubricFamily.MappingRule.feedbackPerspective` + `_mapping.yaml`/`experience-technical-rubric.yaml` 키 + `RubricLoader` 파싱 키 → `rubricCategory` | `backend` | `code-reviewer-backend` | T2 | `RubricLoaderTest` GREEN, `grep "feedbackPerspective" rubric/*.yaml` = 0 |
| 10 | 테스트 일괄 갱신 + grep 검증 + `./gradlew build && test` GREEN | `backend` | `code-reviewer-backend` | T1~T9 | grep 5종 0건, 빌드/테스트 GREEN |

상세는 `tasks/be-NN-*.md` 참조.

### 병렬 가능

- T1, T2, T5 동시 시작 가능 (서로 독립). T1+T2 = enum rename 양쪽 분리 도메인.
- T3 = T2 후 (`RubricFamily.MappingRule` 가 `RubricCategory` 타입 확정 후 `QuestionSet.category` 타입 교체 시 빌드 안정).
- T4 = T1 후 (`AnswerFeedbackPerspective` 시그니처 확정 후).
- T6 = T1 후 (개념상 `selectedAnswerFeedbackPerspective` 가 `AnswerFeedbackPerspective` 단어 따라감 — 단, 필드 타입은 String 유지 → 실제로는 T1 무관하게도 가능. 이름 일관성 위해 T1 후로 정렬).
- T7, T8 = T2 + T3 후 (T7 만 T3 의존, T8 은 T2 만 의존 → 사실상 T7 보다 T8 먼저 가능).
- T9 = T2 후 (`RubricCategory` 타입 확정 후 단어 변경).
- T10 = 모든 task 후 grep + 빌드.

## Verification (BE 범위)

### 테스트 (`backend/.claude/rules/testing.md` 카테고리)

- [ ] **Domain Unit** (≥60% 비중):
  - `QuestionTypeTest` — `RubricCategory` 임포트 갱신, 7개 매핑 단언 GREEN
  - `AnswerResponseTest` — `rubricCategory` 필드 + `InterviewType` 파라미터 시그니처 GREEN
  - `AskedPerspectivesTest` — `AnswerFeedbackPerspective` 제네릭 타입 GREEN
  - `AnswerAnalysisTest`, `AnswerAnalyzerTest` — `AnswerFeedbackPerspective` 임포트 갱신 GREEN
- [ ] **Service Integration**:
  - `RubricLoaderTest` — YAML 파싱 키 `rubricCategory:` + `InterviewType.*` 매칭 GREEN
  - `StandardTrackQuestionGeneratorTest` — `RubricCategory` 임포트 갱신 GREEN
  - `FollowUpServiceTest` — `selectedAnswerFeedbackPerspective` Builder 호출 GREEN
  - `PlaygroundModeHandlerTest`, `ResumeInterviewOrchestratorTest` — `FollowUpExchange.answerText` cascade GREEN
- [ ] **E2E (RestAssured)** — 인터뷰 / 피드백 / followup 기존 E2E 1건씩 통과. JSON 응답 키 검증 (`rubricCategory`, `selectedAnswerFeedbackPerspective`, 요청 키 `answerText`).
- [ ] **Repository / Smoke / ArchUnit**: 변경 영향 없음 → 기존 GREEN 유지 확인.

### 빌드

- [ ] `./gradlew compileJava` 통과 (각 task 후 권장).
- [ ] `./gradlew build` 통과 (T10).
- [ ] `./gradlew test` 통과 (T10).

### grep 검증 (boolean — handoff `참고 명령` 4개 + 추가 1개)

- [ ] `grep -rn "QuestionSetCategory" backend/src/main/java` = 0
- [ ] `grep -rn "import com.rehearse.api.domain.feedback.rubric.entity.RubricCategory" backend/src/main/java` ≥ 6 (임포트 갱신 확인)
- [ ] `grep -rn "import com.rehearse.api.domain.feedback.entity.FeedbackPerspective" backend/src/main/java` = 0
- [ ] `grep -rn "import com.rehearse.api.domain.interview.entity.Perspective\b" backend/src/main/java` = 0
- [ ] `grep -rEn "private static String (formatPerspectives|toReferenceLabel)" backend/src/main/java` = 0
- [ ] `grep -rn "selectedPerspective" backend/src/main/java` = 0
- [ ] `grep -n "feedbackPerspective" backend/src/main/resources/rubric/*.yaml` = 0 (`_mapping.yaml` + `experience-technical-rubric.yaml`)
- [ ] `grep -n "rubricCategory" backend/src/main/resources/rubric/_mapping.yaml` ≥ 1 (키 변경 확인)

### 회귀 체크

- [ ] `FollowUpExchange` JSON 송신 키 = `answerText` (FE 송신 키와 동일 — wire 일치 확인).
- [ ] `_mapping.yaml` / `experience-technical-rubric.yaml` 키 변경 후 `RubricLoaderTest` GREEN.
- [ ] `question_set.category` 컬럼 = 기존 row 호환 (Testcontainers + Flyway 자동 적용 → `@Enumerated(STRING)` 동일).

## Pre / Post State (BE)

### Pre
- enum 양쪽 활성: `Perspective` (interview, 7) + `FeedbackPerspective` (feedback, 3).
- 분류 enum 양쪽 활성: `QuestionSetCategory` (12) + `InterviewType` (12) — 동일 12값.
- 변환 함수 정의 8곳 (formatPerspectives 5 + toReferenceLabel 3) + 호출 9곳.
- `FollowUpExchange.answer` outlier (vs `answerText` 표준 다수).
- `selectedPerspective` 5곳 (`FollowUpRequest$FollowUpExchange`, `FollowUpResponse`, `FollowUpService`, `GeneratedFollowUp` x2).
- `AnswerResponse.from(QuestionAnswer, QuestionSetCategory)` 시그니처.
- DTO `feedbackPerspective` JSON 키 (BE 응답).
- YAML `feedbackPerspective:` 키 (`_mapping.yaml:38`, `experience-technical-rubric.yaml:9`).
- `RubricFamily.MappingRule(_, _, feedbackPerspective, _)` record 컴포넌트 + `RubricLoader` 키 파싱.

### Post
- `AnswerFeedbackPerspective` (interview, 7) + `RubricCategory` (feedback/rubric/entity, 3) — 도메인별 단어 분리. Perspective 단어 = AnswerFeedbackPerspective 내 잔존 (사용자 수용).
- `QuestionSetCategory` 삭제. `QuestionSet.category : InterviewType` (`@Enumerated(STRING)` 유지). DDL/DML 0.
- `infra/ai/prompt/PromptFormatters` (NEW, final + private 생성자) 단일 출처. 8 정의 → 1 클래스 + 9 호출 → `PromptFormatters.formatPerspectives(...)` / `PromptFormatters.toReferenceLabel(...)`.
- `FollowUpExchange.answerText` 통일 (필드 + 생성자 2 + Lombok `.answerText()` getter cascade 15 파일 + JSON 키).
- `selectedAnswerFeedbackPerspective` 일괄 (필드 + JSON 키 + Builder 호출).
- `AnswerResponse.from(QuestionAnswer, InterviewType)` 시그니처.
- DTO `rubricCategory` JSON 키.
- YAML `rubricCategory:` 키 (`_mapping.yaml`, `experience-technical-rubric.yaml`).
- `RubricFamily.MappingRule.rubricCategory` + `RubricLoader` 파싱 키 동기화.
- DB 스키마 변경 0.

## 머지 순서 / 운영 윈도우

- BE PR 1개 (Task 1~10 단일 PR, 10 commit).
- BE 머지 직후 FE PR 즉시 연속 머지 (수분 윈도우 수용 — handoff `머지 윈도우` 결정).
- `@JsonAlias` 호환 레이어 미사용 (handoff 결정).

## 위험 / 롤백

- **`FollowUpExchange` getter cascade 누락 위험**: 15 파일 중 1곳 누락 시 컴파일 RED. **완화** = IntelliJ Safe Rename + Task 5 후 `./gradlew compileJava` 즉시 검증.
- **YAML 키 누락 위험**: `_mapping.yaml` 만 변경하고 `experience-technical-rubric.yaml` 누락 시 `feedbackPerspective` 잔존. **완화** = grep 검증 (Task 10) + Task 9 본문 명시.
- **JSON 키 변경 윈도우**: BE 머지 직후 FE 머지 즉시 (사용자 결정). FE 측 fallback (`?? null`) 또는 사용자 재시도로 깨짐 최소화.
- **롤백**: 회귀 발견 시 항목별 revert. tech-spec L390 `해당 항목 Phase 2 이관` 룰 적용.

## 작업 완료 후 (구현 PR 생성 시)

본 plan = **문서 작성만**. 코드 변경 / 브랜치 생성 / PR 생성 / 커밋 모두 다음 세션에서 진행. 본 세션은 `implement-be.md` + `tasks/be-NN-*.md` 문서만 출력.
