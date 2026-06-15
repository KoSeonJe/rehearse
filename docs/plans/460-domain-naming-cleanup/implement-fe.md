# Implement-FE — 도메인 네이밍 충돌 정리 (Phase 1)

> **작성자**: `frontend` 에이전트
> **선행**: `tech-spec.md` 사용자 승인 + BE PR 머지 (FE 는 BE 머지 직후 즉시 연속 머지)
> **scope**: BE Phase 1 변경에 대한 FE wire / 타입 / 테스트 동기화. 코드 식별자 변경만, 동작 0.

---

## Why

BE Phase 1 = JSON 요청/응답 키 변경 동반. FE 가 BE 와 동시에 wire 키 / 타입 명을 바꿔야 deploy gap 동안의 incompatibility 윈도우 최소화 (사용자 결정 = JsonAlias 호환 레이어 미사용 + 즉시 연속 머지 수용).

Phase 1 FE 변경 사항 (`tech-spec.md` 기준):
- 송신 키: `previousExchanges[].answer` → `answerText`, `selectedPerspective` → `selectedAnswerFeedbackPerspective`.
- 수신 키: `feedbackPerspective` → `rubricCategory`, `selectedPerspective` → `selectedAnswerFeedbackPerspective`.
- 타입: `FeedbackPerspective` → `RubricCategory`.
- 동작 변경 0.

## Goal (측정 가능)

- [ ] `npm run lint` GREEN.
- [ ] `npm run build` GREEN (tsc strict).
- [ ] `npm run test` GREEN.
- [ ] `grep -rn "selectedPerspective" frontend/src` = 0건.
- [ ] `grep -rn "feedbackPerspective" frontend/src` = 0건.
- [ ] `grep -rn "FeedbackPerspective" frontend/src` = 0건.
- [ ] `grep -rn "rubricCategory" frontend/src` ≥ 1 (타입/필드 갱신 확인).
- [ ] `previousExchanges` 송신 객체 키 = `answerText` (use-answer-flow.ts:342-346).
- [ ] BE 머지 직후 dev 환경 통합 동작 확인: 인터뷰 답변 송신 + followup 응답 수신 + ContentTab perspective 표시 정상.

## Evidence (영향 파일)

`grep` 검증 결과 (BE-spec 변경 키 매핑):

| 파일 | 라인 | 현재 | Phase 1 후 |
|------|------|------|-----------|
| `frontend/src/types/interview.ts` | 170 | `export type FeedbackPerspective = ...` | `export type RubricCategory = ...` |
| `frontend/src/types/interview.ts` | 173 | `perspective: FeedbackPerspective \| null` | `rubricCategory: RubricCategory \| null` (TechnicalFeedback) |
| `frontend/src/types/interview.ts` | 263-268 | `FollowUpExchange.answer: string` | `FollowUpExchange.answerText: string` |
| `frontend/src/types/interview.ts` | 274 | `previousExchanges?: Array<{ ..., answer, ... }>` | `Array<{ ..., answerText, selectedAnswerFeedbackPerspective?, ... }>` |
| `frontend/src/types/interview.ts` | 279-290 | `FollowUpResponse` (selectedPerspective 미정의) | 응답에 `selectedAnswerFeedbackPerspective?: string \| null` 추가 (현 FE 미사용 → 유지 옵션 / 사용자 결정) |
| `frontend/src/hooks/use-answer-flow.ts` | 342-346 | `{ question, answer: e.answer, followUpType }` | `{ question, answerText: e.answerText, followUpType }` |
| `frontend/src/stores/interview-store.ts` | 35 | `Map<number, FollowUpExchange[]>` (타입 cascade) | 동일 (필드명 cascade) |
| `frontend/src/stores/interview-store.ts` | 222-226 | `{ question, answer: answerText, type, followUpType }` | `{ question, answerText, type, followUpType }` |
| `frontend/src/components/feedback/content-tab.tsx` | 1, 22, 38, 42-44 | `FeedbackPerspective` 임포트 / `perspective` prop / case 비교 | `RubricCategory` / `rubricCategory` prop 4곳 |
| `frontend/src/components/feedback/__tests__/content-tab.test.tsx` | 7, 23, 40, 65, 91 | fixture `perspective: ...` 5곳 | `rubricCategory: ...` 5곳 |

확인 사항:
- FE는 `AnswerResponse` (`/answers` POST 응답) **미수신** (grep: `frontend/src/hooks/use-answer-flow.ts:152` 등은 응답 body 사용 X) → BE의 `AnswerResponse.feedbackPerspective` JSON 키 변경은 FE 영향 0.
- FE는 `FollowUpResponse.selectedPerspective` **현재 미사용** (grep 결과 0건). BE 응답 키만 변경되며 FE는 type 정의 추가 여부만 결정 (사용처 부재).
- `useFollowUpQuestion` 테스트 (`use-follow-up-question.test.tsx:39`) `previousExchanges: []` = 빈 배열 → 키 변경 무관. fixture 갱신 불필요.

## Trade-offs

### Trade-off 1: FE 응답 타입에 `selectedAnswerFeedbackPerspective` 필드 정의 추가 여부

#### Option A (채택): `FollowUpResponse` 에 `selectedAnswerFeedbackPerspective?: string | null` 정의 추가
- 장점: BE 응답 schema 와 정확히 일치. 추후 사용처 발생 시 타입 안전성.
- 단점: 현재 미사용 → YAGNI 위반 우려. 그러나 schema mirror 는 typed contract 자체.
- 사유: types/interview.ts 가 BE wire 의 단일 진실 — 응답 schema 누락 시 추후 사용 시 `as` 단언 유발.

#### Option B (폐기): 응답 schema 에 미정의 (현재처럼 누락)
- 장점: YAGNI 원칙.
- 폐기 사유: contract 깨짐. 언제든 사용처 추가 시 `as unknown as ...` 단언 유발 + 불일치 noise.

### Trade-off 2: `TechnicalFeedback.perspective` → `rubricCategory` (필드명 자체 변경)

#### Option A (채택): 필드명 변경 (BE inner DTO 의 JSON 키 변경에 동기화)
- 장점: BE wire 와 1:1 대응. 도메인 단어 통일 ("같은 의미면 같은 이름").
- 단점: ContentTab 4곳 + 테스트 5곳 수정.
- 사유: tech-spec BE 결정 = inner field `perspective` JSON 키 → `rubricCategory`. FE 도 동일 키로 수신 매핑 필수.

#### Option B (폐기): FE 만 `perspective` 유지 + zod 매핑 레이어
- 폐기 사유: 단일 식별자 통일 원칙 위반 + zod mapping 추가 코드. 2번 변환.

## Tasks

### 의존 관계

```
fe-01 (types) → fe-02 (hooks) → fe-03 (stores) → fe-04 (components) → fe-05 (tests)
```

순차 구현 (parallel 불가 — 타입 변경이 cascade 컴파일 영향).

### Task fe-01: 타입 정의 갱신
- **Implement**: `frontend`
- **Review**: `code-reviewer-frontend`
- **파일**: `frontend/src/types/interview.ts`
- **변경**:
  - line 170: `export type FeedbackPerspective` → `export type RubricCategory`
  - line 173 (TechnicalFeedback): `perspective: FeedbackPerspective | null` → `rubricCategory: RubricCategory | null`
  - line 263-268 (`FollowUpExchange`): 필드 `answer: string` → `answerText: string`
  - line 274 (`FollowUpRequest.previousExchanges`): `Array<{ question; answer; followUpType? }>` → `Array<{ question; answerText; followUpType?; selectedAnswerFeedbackPerspective?: string | null }>` (selectedAnswerFeedbackPerspective 는 BE wire schema 에 등장 — tech-spec line 287)
  - line 279-290 (`FollowUpResponse`): `selectedAnswerFeedbackPerspective?: string | null` 추가 (Trade-off 1 Option A)
- **테스트**: 본 task 단독 테스트 없음. 후속 task 가 컴파일 에러로 사용처 강제 갱신.
- **완료 기준**: `npm run build` 가 사용처 (use-answer-flow / interview-store / content-tab) 컴파일 에러로 멈춤 (예상 동작).
- **커밋**: `refactor(FE): FollowUp/Feedback 타입을 BE Phase 1 wire schema 에 동기화 (RubricCategory 등)`

### Task fe-02: use-answer-flow 송신 키 변경
- **Implement**: `frontend`
- **Review**: `code-reviewer-frontend`
- **파일**: `frontend/src/hooks/use-answer-flow.ts`
- **변경**:
  - line 342-346: `previousExchanges = history.map((e) => ({ question: e.question, answer: e.answer, followUpType: e.followUpType ?? e.type }))` → `({ question: e.question, answerText: e.answerText, followUpType: e.followUpType ?? e.type })`
  - 송신 키 `answer` → `answerText` (BE FollowUpRequest$FollowUpExchange.answerText 와 일치).
- **테스트**: `frontend/src/hooks/__tests__/use-follow-up-question.test.tsx` = `previousExchanges: []` 빈 배열 (line 39) — 키 변경 무관. 기존 테스트 GREEN 유지.
- **완료 기준**: `npm run lint` + `npm run build` GREEN.
- **커밋**: `refactor(FE): followup previousExchanges 송신 키 answer→answerText`

### Task fe-03: interview-store FollowUpExchange 객체 키 갱신
- **Implement**: `frontend`
- **Review**: `code-reviewer-frontend`
- **파일**: `frontend/src/stores/interview-store.ts`
- **변경**:
  - line 222-226 `completeFollowUpRound`: `{ question: currentFollowUp.question, answer: answerText, type: currentFollowUp.type, followUpType: currentFollowUp.type }` → `{ question, answerText, type, followUpType }` (필드명 단순화 + `answer` → `answerText`).
  - 타입 `FollowUpExchange` 변경 (fe-01) cascade.
- **테스트**: 본 store 직접 unit 테스트 없음. fe-04 / fe-05 통합 테스트로 검증.
- **완료 기준**: `npm run build` GREEN.
- **커밋**: `refactor(FE): interview-store FollowUpExchange 객체 키 answer→answerText`

### Task fe-04: ContentTab perspective → rubricCategory
- **Implement**: `frontend`
- **Review**: `code-reviewer-frontend`
- **파일**: `frontend/src/components/feedback/content-tab.tsx`
- **변경**:
  - line 1: import `FeedbackPerspective` → `RubricCategory`.
  - line 22 `resolveCopy`: 파라미터 타입 `FeedbackPerspective | null` → `RubricCategory | null`. 파라미터명 `perspective` → `rubricCategory` (식별자 단어 통일).
  - line 38: `technicalFeedback?.perspective` → `technicalFeedback?.rubricCategory`.
  - line 42-44: `technicalFeedback.perspective === 'TECHNICAL' || ...` → `technicalFeedback.rubricCategory === 'TECHNICAL' || ...`.
- **테스트**: fe-05 에서 fixture 갱신.
- **완료 기준**: `npm run build` GREEN.
- **커밋**: `refactor(FE): ContentTab perspective→rubricCategory 식별자 통일`

### Task fe-05: 테스트 fixture 갱신 + 회귀 검증
- **Implement**: `frontend`
- **Review**: `code-reviewer-frontend`
- **파일**: `frontend/src/components/feedback/__tests__/content-tab.test.tsx`
- **변경**:
  - line 7 `buildFeedback` 기본값: `perspective: 'TECHNICAL'` → `rubricCategory: 'TECHNICAL'`.
  - line 23, 40, 65, 91: `buildFeedback({ perspective: ... })` → `buildFeedback({ rubricCategory: ... })` (4곳).
  - 테스트 `it()` 라벨 / 한국어 한 줄 = 의미 보존 (UI 라벨 / 도메인 단어 변경 없음 — Phase 2 영역). 라벨 본문 그대로.
- **검증**:
  - `npm run lint && npm run build && npm run test` GREEN.
  - grep 검증 명령 (`Verification` 섹션) 모두 0건.
- **완료 기준**: 위 검증 + 다음 회귀 체크 통과:
  - 인터뷰 답변 1회 송신 → BE followup 응답 정상 매핑 (dev 환경, BE 머지 후).
  - 피드백 화면 ContentTab 4가지 케이스 (TECHNICAL / EXPERIENCE / BEHAVIORAL / null) 라벨 정상 노출.
- **커밋**: `test(FE): ContentTab fixture perspective→rubricCategory + 회귀 검증`

## Verification

### 빌드 / 테스트 (Blocking)

```bash
cd frontend
npm run lint            # ESLint 0 error
npm run build           # tsc strict 0 error + vite build OK
npm run test            # vitest run 모든 suite GREEN
```

### grep 검증 (각 0건이어야 통과)

```bash
grep -rn "selectedPerspective" frontend/src
grep -rn "feedbackPerspective" frontend/src
grep -rn "FeedbackPerspective" frontend/src
grep -rn "rubricCategory" frontend/src   # ≥1 (타입/필드 존재 확인)
grep -rn "previousExchanges.*answer:" frontend/src   # answer 키 잔존 0
grep -rn "perspective: FeedbackPerspective" frontend/src   # 0
grep -rn "\.perspective" frontend/src/components/feedback   # 0
```

### 회귀 (BE 머지 직후 dev 통합)

| 시나리오 | 기대 |
|---------|------|
| 인터뷰 시작 → 답변 제출 | followup 요청 송신 키 `answerText` 직렬화 정상 |
| followup 응답 수신 | BE 응답에 `selectedAnswerFeedbackPerspective` 키 매핑 (현재 사용처 없음 — undefined 무영향) |
| 피드백 화면 진입 | TimestampFeedback.technicalFeedback.rubricCategory 키 매핑 → ContentTab 라벨 정상 (TECHNICAL / EXPERIENCE / BEHAVIORAL / null 4-state) |
| Loading / Error / Empty | 4-state 깨짐 0 (라벨 토큰 변경 없음) |

## Pre / Post State

### Pre (현재 — 2026-05-09)

```typescript
// frontend/src/types/interview.ts
export type FeedbackPerspective = 'TECHNICAL' | 'EXPERIENCE' | 'BEHAVIORAL'
export interface TechnicalFeedback {
  perspective: FeedbackPerspective | null
  ...
}
export interface FollowUpExchange {
  question: string
  answer: string                    // ← BE FollowUpExchange.answer 와 일치 (변경 전 broken X)
  type: FollowUpType
  followUpType?: FollowUpType
}
export interface FollowUpRequest {
  previousExchanges?: Array<{ question: string; answer: string; followUpType?: FollowUpType }>
  ...
}
// FollowUpResponse 에 selectedAnswerFeedbackPerspective 미정의

// frontend/src/hooks/use-answer-flow.ts:342-346
const previousExchanges = history.map((e) => ({
  question: e.question,
  answer: e.answer,                 // ← 변경 대상
  followUpType: e.followUpType ?? e.type,
}))

// frontend/src/components/feedback/content-tab.tsx:38
const copy = resolveCopy(technicalFeedback?.perspective ?? null)
```

### Post (구현 후)

```typescript
// frontend/src/types/interview.ts
export type RubricCategory = 'TECHNICAL' | 'EXPERIENCE' | 'BEHAVIORAL'
export interface TechnicalFeedback {
  rubricCategory: RubricCategory | null
  ...
}
export interface FollowUpExchange {
  question: string
  answerText: string                // ← BE answerText 와 일치
  type: FollowUpType
  followUpType?: FollowUpType
}
export interface FollowUpRequest {
  previousExchanges?: Array<{
    question: string;
    answerText: string;
    followUpType?: FollowUpType;
    selectedAnswerFeedbackPerspective?: string | null;
  }>
  ...
}
export interface FollowUpResponse {
  ...
  selectedAnswerFeedbackPerspective?: string | null
}

// frontend/src/hooks/use-answer-flow.ts:342-346
const previousExchanges = history.map((e) => ({
  question: e.question,
  answerText: e.answerText,
  followUpType: e.followUpType ?? e.type,
}))

// frontend/src/components/feedback/content-tab.tsx:38
const copy = resolveCopy(technicalFeedback?.rubricCategory ?? null)
```

| 항목 | Pre | Post |
|------|-----|------|
| 타입 alias | `FeedbackPerspective` | `RubricCategory` |
| TechnicalFeedback 필드 | `perspective` | `rubricCategory` |
| FollowUpExchange 필드 | `answer` | `answerText` |
| previousExchanges 송신 키 | `answer` | `answerText` |
| FollowUpResponse 필드 | `selectedAnswerFeedbackPerspective` 미정의 | `selectedAnswerFeedbackPerspective?: string \| null` 추가 |
| ContentTab prop | `perspective` (4곳) | `rubricCategory` (4곳) |
| 테스트 fixture | `perspective:` (5곳) | `rubricCategory:` (5곳) |
| 동작 / UI 라벨 텍스트 | "기술 피드백" / "경험 평가" / "경험/협업" / fallback | 동일 (변경 없음) |

## 위험 / 롤백

### 위험

- **BE/FE 머지 윈도우** (사용자 결정 = 수용): BE 머지 후 FE 머지 전 윈도우 동안 dev 환경 followup 요청 = `answer` 키 송신 → BE 가 `answerText` 기대 → 역직렬화 시 null. **완화** = BE PR 머지 직후 FE PR 즉시 연속 머지 (수분 윈도우). 사용자 재시도로 복구.
- **응답 키 불일치 윈도우**: BE 가 `rubricCategory` 응답 + FE 가 `perspective` 키 매핑 시 ContentTab fallback 진입 ("해당 턴은 평가 대상이 아닙니다."). 동일 머지 윈도우 내 한정.
- **타입 cascade 누락**: fe-01 (types) 변경 후 fe-02~04 누락 시 컴파일 에러로 즉시 발견. 안전.

### 롤백

- 각 task 단위 commit → 회귀 발견 시 해당 task revert.
- 본 phase 1 항목 회귀 발생 시 = product-spec AC 룰에 따라 해당 항목 Phase 2 이관 + 나머지 머지.

## 분기 결정

- [x] BE+FE 동시 (BE 선행 머지 + FE 즉시 연속 머지)
- 본 문서 = FE 측 단일 파일 plan (task 5개 < 임계 8) — `tasks/fe-NN-*.md` 분리 불요.

## 참고 명령

```bash
# 브랜치 (BE 머지 후 develop 동기화 → 본 FE 브랜치 base 갱신)
git checkout develop
git pull origin develop
git checkout -b feat/460-domain-naming-cleanup-fe

# 빌드 + 테스트
cd frontend
npm run lint && npm run build && npm run test

# grep 검증 (구현 후 0건 확인)
grep -rn "selectedPerspective\|feedbackPerspective\|FeedbackPerspective" frontend/src
```
