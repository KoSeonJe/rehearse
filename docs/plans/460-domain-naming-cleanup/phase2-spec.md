# Phase 2 Spec — 460 도메인 단어 충돌 잔여 정리

> **위치**: `docs/plans/460-domain-naming-cleanup/phase2-spec.md` (Phase 1 폴더 내 통합 spec)
> **상태**: 작성 — 사용자 승인 대기
> **대상 카탈로그 항목** (Phase 1 비스코프 잔여): #2 MODEL_ANSWER 정합성 / #3 EXPERIENCE 단어 충돌 / #12 ReferenceType 코드↔프롬프트 단어 불일치
> **선행**: Phase 1 (BE PR #464 / FE PR #465) 머지 완료. develop HEAD `277ab55`.

---

## Why

- `Question.modelAnswer` (실제 모범답변 텍스트 컬럼) 와 `ReferenceType.MODEL_ANSWER` (참조 타입 enum 값) **동명 충돌** — 코드만 보면 의미 분간 어려움.
- `ReferenceType` 코드 (`MODEL_ANSWER` / `GUIDE`) 와 프롬프트 라벨 (`CONCEPT` / `EXPERIENCE`) **단어 갈라짐** — `PromptFormatters.toReferenceLabel` 변환 레이어 필요. 코드↔프롬프트 사상 추론 어려움.
- 프롬프트 라벨 `EXPERIENCE` 가 `RubricCategory.EXPERIENCE` (채점 카테고리) 와 우연히 동명 — 의미 다른 두 단어가 한 컨텍스트에서 충돌.

## Goal

- `grep -rn "modelAnswer\|model_answer" backend/src/main` = 0 (`bestAnswer` 만 잔존).
- `ReferenceType` enum 값 = 프롬프트 라벨 1:1 일치 (변환 레이어 = identity).
- BE 단위/통합/E2E + Live LLM E2E + FE 빌드/테스트 모두 GREEN.
- 동작 변경 0 — 식별자/컬럼명/프롬프트 라벨만 변경.

## Scope

### α. 컬럼/필드 rename (`modelAnswer` → `bestAnswer`)
- DB 컬럼 (2 테이블): `question.model_answer`, `question_pool.model_answer` → `best_answer`
  - `prepared_follow_up` 테이블은 V12 에서 이미 DROP — 본 spec 대상 아님
- Entity 필드 / getter 일괄 cascade
- API 응답 DTO JSON 키 동시 (FE wire 변경)

### β. 프롬프트 라벨 일치 (`CONCEPT`/`EXPERIENCE` → `MODEL_ANSWER`/`GUIDE`)
- `PromptFormatters.toReferenceLabel` 매핑 변경
- 프롬프트 빌더 (`AnswerAnalyzerPromptBuilder`, `AudioTurnAnalyzerPromptBuilder`, `FollowUpPromptBuilder`) 영향 검토 — 코드는 toReferenceLabel 결과만 삽입 → 대부분 자동 반영

### γ. LLM 응답 schema 키 통일 (`model_answer` JSON 키 → `best_answer`)
- `GeneratedQuestion.modelAnswer`, `GeneratedFollowUp.modelAnswer` `@JsonProperty("model_answer")` → `best_answer`
- 프롬프트 템플릿 (`prompts/template/*.txt`, `prompts/template/resume/*.txt`) 응답 schema 명시 부분 동시 수정 (6 파일)
- `SchemaExampleRegistry`, `MockAiClient` 예시 JSON 동기화

## 변경 매핑 표

| 카테고리 | Before | After |
|---------|--------|-------|
| DB 컬럼 | `question.model_answer`, `question_pool.model_answer` | `best_answer` (2 테이블, `prepared_follow_up` 은 V12 DROP 완료) |
| Entity 필드 | `Question.modelAnswer`, `QuestionPool.modelAnswer` | `bestAnswer` |
| Domain 헬퍼 / Service | `ResumeQuestionResultGenerator.modelAnswer()`, `QuestionSetAssembler` 등 | `bestAnswer` cascade |
| API 응답 DTO 필드 + JSON 키 | `AnswerResponse.modelAnswer`, `FollowUpResponse.modelAnswer`, `QuestionDetailResponse.modelAnswer`, `QuestionsWithAnswersResponse.modelAnswer`, `TimestampFeedbackResponse.modelAnswer`, `ReviewBookmarkListItem.modelAnswer` | `bestAnswer` |
| LLM 응답 DTO | `GeneratedQuestion.modelAnswer` (`@JsonProperty("model_answer")`), `GeneratedFollowUp.modelAnswer` | `bestAnswer` (`@JsonProperty("best_answer")`) |
| `ReferenceType` enum 값 | `MODEL_ANSWER` / `GUIDE` | **유지** (변경 없음) |
| 프롬프트 라벨 (`PromptFormatters.toReferenceLabel`) | `MODEL_ANSWER` → `"CONCEPT"`, `GUIDE` → `"EXPERIENCE"` | `MODEL_ANSWER` → `"MODEL_ANSWER"`, `GUIDE` → `"GUIDE"` (identity) |
| 프롬프트 템플릿 (`prompts/template/*.txt`) | `"model_answer": "..."` JSON 키 | `"best_answer": "..."` |

## 영향 범위 (파일 그룹)

- **BE 프로덕션 코드**: 21 파일 (`domain/feedback`, `domain/interview`, `domain/question`, `domain/resume`, `domain/reviewbookmark`, `infra/ai`)
- **BE 테스트**: 15 파일 (단위 + 통합 + Live LLM E2E)
- **프롬프트 템플릿**: 6 파일 (`prompts/template/`)
- **DB 마이그**: V47 신규 1개 (2 테이블 `RENAME COLUMN`). DDL only, backfill 0
- **DB seed**: 22 파일 (`db/seed/*.sql`) — INSERT 컬럼 리스트 갱신
- **k6 / wiremock**: 3 파일 (`src/test/k6/`)
- **FE wire**: 8 파일 (`types/`, `components/feedback/`, `components/review/`, `hooks/`, `pages/interview-analysis-page.tsx`, `__tests__/use-answer-flow.test.tsx`)

## 작업 분해 (BE / FE 분리)

### BE PR (단일 브랜치 `feat/{N}-modelanswer-rename`)
- **commit 1** [DB]: V47 마이그 (`RENAME COLUMN model_answer TO best_answer` × 2 테이블) + Entity `@Column` 갱신
- **commit 2** [Entity/Service]: `Question.modelAnswer` → `bestAnswer` getter cascade (도메인 17 파일)
- **commit 3** [DTO/API 응답]: `AnswerResponse` 등 6 응답 DTO 필드 + JSON 키
- **commit 4** [LLM schema]: `GeneratedQuestion`, `GeneratedFollowUp` `@JsonProperty` + 프롬프트 템플릿 6 파일 응답 schema 키
- **commit 5** [Seed/k6]: 22 seed SQL + k6 mock + wiremock stub (γ INSERT 컬럼 리스트 갱신)
- **commit 6** [PromptFormatters β]: `toReferenceLabel` identity 화 + 프롬프트 템플릿 정적 라벨 (`QUESTION_REFERENCE_TYPE: CONCEPT/EXPERIENCE` → `MODEL_ANSWER/GUIDE`, 4 템플릿) + 관련 테스트 cascade
- **commit 7** [Tests α/γ]: α/γ 영향 테스트 파일 cascade (β 외)

### FE PR (단일 브랜치 `feat/{N}-modelanswer-rename-fe`)
- **commit 1**: `types/interview.ts`, `types/review-bookmark.ts` 필드 rename
- **commit 2**: `components/feedback/feedback-panel.tsx`, `components/review/*` (2 파일), `hooks/use-answer-flow.ts`, `pages/interview-analysis-page.tsx` cascade
- **commit 3**: `__tests__/use-answer-flow.test.tsx` 갱신

### 머지 윈도우
- BE PR 머지 직후 FE PR 즉시 연속 머지 (Phase 1 동일 패턴). JsonAlias 호환 미사용.

## Acceptance Criteria

- [ ] `grep -rn "modelAnswer\|model_answer" backend/src/main backend/src/test backend/src/main/resources` = 0 (`bestAnswer` / `best_answer` 만 잔존).
- [ ] `grep -rn "modelAnswer" frontend/src` = 0 (`bestAnswer` 만 잔존).
- [ ] `PromptFormatters.toReferenceLabel(MODEL_ANSWER)` = `"MODEL_ANSWER"`, `toReferenceLabel(GUIDE)` = `"GUIDE"` (identity).
- [ ] `./gradlew build && ./gradlew test` GREEN.
- [ ] `RUN_LIVE_API=true ./gradlew test --tests "*Live*"` (특히 `ResumeChainInterrogatorLiveLlmE2ETest`, `ResumePlaygroundLiveLlmE2ETest`) 통과.
- [ ] FE `npm run lint && npm run build && npm run test` GREEN.
- [ ] dev 환경 smoke (인터뷰 생성 → 답변 → 피드백 응답 JSON `bestAnswer` 키 확인).

## 비스코프 (Don't)

본 spec **절대 미터치**:

- **#10 AskedPerspectives 표현** — 도메인 모델링 결정 선행 필요. 별 spec.
- **#11 ResumeMode 계층** — `@Enumerated(STRING)` DB 마이그 + #458 (Resume skeleton redesign) 의존. 별 plan.
- **#13 ArchUnit 룰** — 회귀 차단 도구. 별 작업.
- 그 외 도메인 식별자 추가 rename / 리팩.

## 위험 / 롤백

| 위험 | 사유 | 대응 |
|------|------|------|
| LLM 응답 schema 변경 (`best_answer` 키) | GPT 응답 품질 변화 가능 — 키 단어가 토큰 분포에 영향 | Live LLM E2E 통과 + RESUME 모범답변 sprint S14 영향 사전 평가. 회귀 발견 시 γ 만 롤백 (α/β 유지) |
| 프롬프트 라벨 `MODEL_ANSWER`/`GUIDE` 단어 변경 | LLM 추론 단어 = 영향 가능 | β 단계 별도 commit 분리 → 회귀 발견 시 β 만 revert 가능 |
| FE 동시 머지 실패 | BE 만 먼저 머지 시 wire 깨짐 (응답 키 변경) | BE PR 머지 즉시 FE PR 연속 머지. `gh pr merge` 시점 align |
| DB 마이그 V47 = `RENAME COLUMN` | DDL 락 — 짧음 (수초). 데이터 backfill 0 | dev 머지 후 prod 배포 전 dev DB 검증 |

## Pre / Post 상태

### Pre (현재 = develop `277ab55`)
- DB 컬럼 `model_answer` × 2 테이블 (`question`, `question_pool`)
- Entity 필드 `modelAnswer`
- API JSON 응답 키 `modelAnswer`
- LLM 응답 schema 키 `model_answer`
- 프롬프트 라벨 `CONCEPT` / `EXPERIENCE` (Formatter 변환)

### Post
- DB 컬럼 `best_answer` × 2 테이블 (`question`, `question_pool`)
- Entity 필드 `bestAnswer`
- API JSON 응답 키 `bestAnswer`
- LLM 응답 schema 키 `best_answer`
- 프롬프트 라벨 `MODEL_ANSWER` / `GUIDE` (Formatter identity)

## 참고

- 460 Phase 1 PR: #464 (BE), #465 (FE)
- 관련 plan: `docs/plans/460-domain-naming-cleanup/` (Phase 1 spec / handoff)
- Issue: 신규 생성 후 본 spec 헤더에 번호 기입 (또는 460 reopen — 사용자 결정)

---

업데이트: 2026-05-09. Phase 1 완료 후 Phase 2 통합 spec 작성.
