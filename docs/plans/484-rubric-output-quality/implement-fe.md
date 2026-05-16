# Implement (Frontend) — rubric 출력 품질 회복 (단일 패턴 통일)

> **작성자**: frontend agent
> **답하는 질문**: FE 어떤 순서로 실행 → DeliveryTab 폐지 + ContentTab 비언어 카드 + Empty 분기 + 폴링 종료 정합?
> **승인 게이트**: ★ `tech-spec.md` API contract 승인 후 시작 ★
> **대응 spec**: `product-spec.md` + `tech-spec.md`

---

## Phase 0: API Contract 확인 + BE 선행 강제

`tech-spec.md#api-contract` (Phase 2a BE → FE 응답) 확정 여부 확인.

- [x] Endpoint — `GET /api/feedbacks/{interviewId}` 응답 확장
- [x] Response schema — `TimestampFeedbackResponse.nonverbalFeedback {rubricId, dimensions[]}` 신설 (verbal `technicalFeedback` 동일 구조)
- [x] top-level `fillerWordCount` 노출 (필러 배지 source 단순화 — P1-A)
- [x] 부분 실패 contract — `nonverbalFeedback === null` (전체) / `dimensions.length < 3` (부분) / `dimensions.length === 3` (정상)
- [x] 폴링 종료 contract — `analysisStatus ∈ {COMPLETED, PARTIAL, FAILED, SKIPPED}` 단일 출처 유지 (`delivery.*` 의존 부재, P0-4)

**BE 선행 강제** — Phase 2a (BE PR#2) 머지 + dev 검증 게이트 통과 후 FE Phase 2b 진입. BE 미머지 상태 mock 진행 X (응답 DTO 시그니처 확정 의존).

---

## Phase / Task 개요

| Task | Phase | 제목 | 구현 | 의존 |
|------|-------|------|------|------|
| FE-01 | 2b | `types/interview.ts` `TimestampFeedback.nonverbalFeedback` 신설 + `DeliveryFeedback.nonverbal` 제거 | `frontend` | BE PR#2 머지 |
| FE-02 | 2b | `RubricDimensionCard` 신규 + `content-tab.tsx` 비언어 카드 그룹 + Empty 분기 (부분 / 전체) | `frontend` | FE-01 |
| FE-03 | 2b | `feedback-panel.tsx` Tabs 제거 + 필러 배지 top-level (`feedback.fillerWordCount`) 전환 + `delivery-tab.tsx` 삭제 | `frontend` | FE-02 |
| FE-04 | 2b | 회귀 테스트 (content-tab / rubric-dimension-card / feedback-panel filler / interview-analysis-page polling — P0-4) | `frontend` | FE-03 |
| FE-05 | 3 | Phase 3 — types / utility (`isCommentBlockEmpty` / `StructuredComment` / `LevelBadge` / `format-feedback-level`) cleanup | `frontend` | BE PR#3 머지 (Phase 3) + 사용자 승인 |

> Task 5개 / 본문 임계 미달 → 단일 implement-fe.md 본문에 상세 기재 (tasks/ 분리 X).

---

## PR 단위

| PR | 포함 Task | 머지 후 게이트 |
|----|-----------|----------------|
| PR#4 (Phase 2b) | FE-01 ~ FE-04 | dev 결과 화면 직접 확인 + 사용자 명시 승인 |
| PR#5 (Phase 3) | FE-05 | grep 0 검증 + 사용자 승인 |

---

## FE-01: `types/interview.ts` nonverbalFeedback 신설

- **구현**: `frontend` — type 정의 / BE 응답 schema 정합

### 변경 파일

- `frontend/src/types/interview.ts` (line 154, 166-198)
  - **신설**: `interface NonverbalFeedback { rubricId: string; dimensions: TechnicalDimensionFeedback[]; }`
    - 기존 `NonverbalFeedback` (line 167 의 `DeliveryFeedback.nonverbal: NonverbalFeedback | null`) 와 충돌 회피 — 기존 type 이름 변경 또는 BE inner class 명 정합 (`NonverbalRubricFeedback`). 본 spec 결정: **기존 `NonverbalFeedback` 을 `LegacyNonverbalFeedback` 으로 rename** (Phase 3 FE-05 에서 전체 삭제) + 신규 type 명 `NonverbalFeedback` (응답 JSON 키 `nonverbalFeedback` 정합).
  - `TimestampFeedback` 에 `nonverbalFeedback: NonverbalFeedback | null` 필드 추가 (line 198 추정)
  - `DeliveryFeedback.nonverbal: LegacyNonverbalFeedback | null` 필드 제거 (interface 자체는 `vocal` / `attitudeComment` 잔존, Phase 3 전체 제거)

### 핵심 로직

```
TimestampFeedback {
  ...기존
  nonverbalFeedback: NonverbalFeedback | null;  // 신규
  // delivery 의 nonverbal 필드 제거. delivery 자체는 Phase 3 까지 잔존.
}
NonverbalFeedback { rubricId: string; dimensions: TechnicalDimensionFeedback[]; }
```

### 의존

- 선행: BE PR#2 머지 (응답 schema 확정)
- 외부: 없음

### Verification

- [ ] `npm run lint`
- [ ] `npm run test -- src/types` (type-only — 컴파일 회귀)
- [ ] `tsc -b` 컴파일 green

### 커밋 메시지

```
feat(FE): TimestampFeedback.nonverbalFeedback type 신설 + DeliveryFeedback.nonverbal 제거
```

---

## FE-02: `RubricDimensionCard` 신규 + `content-tab.tsx` 비언어 카드 그룹

- **구현**: `frontend` — 컴포넌트 분리 / Empty 분기

### 변경 파일

- `frontend/src/components/feedback/rubric-dimension-card.tsx` — **신규 컴포넌트**
  - props: `{ dimension: TechnicalDimensionFeedback }` (≤2 props 룰 정합)
  - `content-tab.tsx:86-112` 의 JSX 추출 (score + observation + evidenceQuote 카드 렌더)
  - Stateless 컴포넌트 — verbal / 비언어 동일 재사용
- `frontend/src/components/feedback/content-tab.tsx` (line 1-119)
  - props 확장: `{ technicalFeedback: TechnicalFeedback | null; nonverbalFeedback: NonverbalFeedback | null; questionType: string | null }` (3 props — 사용자 결정 또는 단일 객체 props 로 묶기)
  - verbal 카드 그룹 + 비언어 카드 그룹 동시 렌더 (`RubricDimensionCard` 재사용)
  - **비언어 영역 분기**:
    - `nonverbalFeedback === null` → "분석 실패 — 점수 없음" Empty 카드
    - `nonverbalFeedback.dimensions.length < 3` → 부분 카드 + "이번 답변 비언어 분석 일부 실패" 1줄 안내
    - 정상 3차원 → 카드 3개 (fluency / confidence_tone / eye_contact_posture)
  - RESUME_OPENER / isCardable Empty 분기 보존

### 핵심 로직

```
<ContentTab>
  ├─ verbal section
  │   ├─ technicalFeedback === null → Empty
  │   └─ technicalFeedback.dimensions → RubricDimensionCard 카드 그룹
  └─ 비언어 section (헤더 "비언어 평가" + rubricId)
      ├─ nonverbalFeedback === null → "분석 실패 — 점수 없음"
      ├─ dimensions.length < 3       → 부분 카드 + "일부 실패" 안내
      └─ dimensions.length === 3     → RubricDimensionCard × 3
```

### 의존

- 선행: FE-01
- 외부: 기존 카드 primitive (shadcn) 재사용

### Verification

- [ ] `npm run lint`
- [ ] `npm run test -- src/components/feedback/rubric-dimension-card.test.tsx`
- [ ] `npm run test -- src/components/feedback/content-tab.test.tsx`

### 커밋 메시지

```
feat(FE): RubricDimensionCard 신규 + content-tab 비언어 카드 그룹 + Empty 분기
```

---

## FE-03: `feedback-panel.tsx` Tabs 제거 + 필러 배지 top-level 전환 + `delivery-tab.tsx` 삭제

- **구현**: `frontend` — Tab 폐지 / 필러 source 단순화 / 파일 삭제

### 변경 파일

- `frontend/src/components/feedback/feedback-panel.tsx` (line 34, 46-55, 145-176)
  - `FeedbackTab = 'content' | 'delivery'` type 제거
  - `Tabs` / `TabsList` / `TabsTrigger` / `TabsContent` 컴포넌트 제거 → 단일 `ContentTab` 렌더
  - `isDeliveryAvailable` / `effectiveTab` 로직 제거
  - `DeliveryTab` import 제거
  - **필러 카운트 배지 (line 111-117 추정 "습관어 N회 감지") 유지** — source 경로 변경:
    - 기존: `feedback.delivery?.vocal?.fillerWordCount`
    - 신규: `feedback.fillerWordCount` (top-level 필드, `TimestampFeedbackResponse.fillerWordCount` 이미 존재)
  - **검증 가이드 (P1-A)**: 구현 진입 직전 `grep -n 'fillerWordCount' backend/src/.../TimestampFeedbackResponse.java` 로 top-level 필드 노출 line 재확인. nested 만 노출 시 = BE Phase 2a 변경 범위 (BE-07) 에 top-level 노출 합류 의뢰.
- `frontend/src/components/feedback/delivery-tab.tsx` — **파일 삭제** (170 lines)

### 핵심 로직

```
[Pre]  <FeedbackPanel>
         └─ Tabs (value: 'content' | 'delivery')
              ├─ ContentTab
              └─ DeliveryTab (자유서술 + raw 측정치 + LevelBadge + filler)

[Post] <FeedbackPanel>
         ├─ FillerBadge (feedback.fillerWordCount > 0 시)
         └─ ContentTab (technicalFeedback + nonverbalFeedback)
```

### 의존

- 선행: FE-02
- 외부: 없음

### Verification

- [ ] `grep -r "DeliveryTab\|delivery-tab" frontend/src` = 0
- [ ] `npm run lint`
- [ ] `npm run test -- src/components/feedback/feedback-panel.test.tsx`
- [ ] `npm run build`

### 커밋 메시지

```
refactor(FE): feedback-panel Tabs 제거 + ContentTab 단일 렌더
refactor(FE): 필러 배지 source 를 feedback.fillerWordCount top-level 로 전환
chore(FE): delivery-tab.tsx 파일 삭제
```

---

## FE-04: 회귀 테스트 (content-tab / rubric-dimension-card / feedback-panel filler / polling P0-4)

- **구현**: `frontend` — RTL + msw 행위 테스트

### 변경 파일

- `frontend/src/components/feedback/__tests__/content-tab.test.tsx` — 신규 / 갱신
- `frontend/src/components/feedback/__tests__/rubric-dimension-card.test.tsx` — 신규
- `frontend/src/components/feedback/__tests__/feedback-panel.test.tsx` — 신규 / 갱신
- `frontend/src/pages/__tests__/interview-analysis-page.test.tsx` (또는 `use-question-sets.test.tsx`) — 폴링 종료 회귀 추가

### 테스트 시나리오

- [ ] **`content-tab.test.tsx.shows_verbal_and_nonverbal_cards_when_both_present`**:
  - `technicalFeedback` + `nonverbalFeedback` 정상 → verbal + 비언어 rubric 카드 3개 렌더 assert
  - `screen.queryByRole('tab')` = 0 (tab navigation 부재)
  - 자유서술 텍스트 (`긍정/부정/제안`) 부재 assert
- [ ] **`content-tab.test.tsx.shows_partial_warning_when_dimensions_lt_3`**:
  - `nonverbalFeedback.dimensions.length === 2` → 카드 2개 + "비언어 분석 일부 실패" 1줄 안내 노출
- [ ] **`content-tab.test.tsx.shows_empty_card_when_nonverbal_feedback_null`**:
  - `nonverbalFeedback === null` → "분석 실패 — 점수 없음" Empty 카드 렌더
- [ ] **`rubric-dimension-card.test.tsx`**:
  - props `{ dimension: {dimension, score, observation, evidenceQuote} }` → 4개 필드 정확 렌더 assert
- [ ] **`feedback-panel.test.tsx.shows_filler_count_badge_when_filler_present`**:
  - `feedback.fillerWordCount === 3` → "습관어 3회 감지" 텍스트 노출
  - `feedback.fillerWordCount === null` 또는 `0` → 배지 부재
  - tab `screen.queryByRole('tab')` = 0
- [ ] **`interview-analysis-page.test.tsx.polling_stops_when_analysis_status_terminal`** (P0-4 회귀):
  - msw status endpoint 점진 응답 (`PENDING → ANALYZING → COMPLETED`) — Lambda 부분 실패 페이로드 수신 시나리오
  - `useAllQuestionSetStatuses` 종료 + `refetchInterval` 호출 횟수 종료 검증
  - `delivery.*` 필드 의존 부재 회귀 보호

### 의존

- 선행: FE-03
- 외부: msw / RTL (기존)

### Verification

- [ ] `npm run test` 전체 green
- [ ] `npm run lint`
- [ ] `npm run build`

### 커밋 메시지

```
test(FE): content-tab / rubric-dimension-card 비언어 카드 회귀
test(FE): feedback-panel filler 배지 source top-level 전환 회귀
test(FE): interview-analysis-page 폴링 종료 (analysisStatus 단일 출처) 회귀
```

---

## FE-05: Phase 3 — types / utility cleanup

- **구현**: `frontend` — type / 잔존 utility 삭제

### 변경 파일

- `frontend/src/types/interview.ts`
  - `DeliveryFeedback` / `LegacyNonverbalFeedback` (FE-01 rename) / `VocalFeedback` / `CommentBlock` type 삭제
  - `TimestampFeedback.delivery` 필드 제거
- `frontend/src/lib/feedback/` (또는 동등 위치) — 잔존 utility 삭제 검증 후 처리:
  - `isCommentBlockEmpty`
  - `StructuredComment` 컴포넌트
  - `LevelBadge` 컴포넌트
  - `format-feedback-level` utility
  - 각 파일 별 참조 grep 0 검증 후 삭제. 부분 잔존 (다른 기능 의존) 시 별도 보고 후 결정.

### 핵심 로직

```
grep -rn "DeliveryFeedback\\|LegacyNonverbalFeedback\\|VocalFeedback\\|CommentBlock" frontend/src = 0
grep -rn "isCommentBlockEmpty\\|StructuredComment\\|LevelBadge\\|format-feedback-level" frontend/src = 0
→ 위 grep 결과 0 인 type / 파일 삭제. 1+ 잔존 시 별도 보고.
```

### 의존

- 선행: BE PR#3 머지 (BE-08 응답 DTO `delivery` 키 부재 확정)
- 외부: 없음

### Verification

- [ ] grep 0 (위 핵심 로직 명령)
- [ ] `npm run lint`
- [ ] `npm run test` 전체 green
- [ ] `npm run build`
- [ ] `tsc -b` 컴파일 green (잔존 참조 0 검증)

### 커밋 메시지

```
chore(FE): DeliveryFeedback / Vocal / CommentBlock type 삭제
chore(FE): StructuredComment / LevelBadge / format-feedback-level utility 삭제
```

---

## 통합 Verification

- [ ] tech-spec.md Verification (Phase 2b + Phase 3 FE 항목) 통과
- [ ] BE 통합 후 회귀 체크:
  - dev 결과 화면 직접 확인 — 단일 흐름 (탭 부재) / 자유서술 카드 부재 / 원시 측정치 카드 부재 (필러 카운트 배지 1건은 유지) / composure 카드 부재 / 비언어 3차원 카드 노출 / 부분 실패 안내 / 폴링 종료 확인
- [ ] grep 0 검증:
  - `grep -r "DeliveryTab\\|delivery-tab" frontend/src` = 0 (FE-03 후)
  - `grep -r "DeliveryFeedback\\|CommentBlock\\|VocalFeedback" frontend/src` = 0 (FE-05 후)
- [ ] FE 테스트 카테고리 단일 채널 (Vitest + RTL + msw) green

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-frontend` 실행 (구현 완료 직후 — 메인 세션 책임)
  - PR#4 (Phase 2b FE-01~FE-04) 머지 직전 1회
  - PR#5 (Phase 3 FE-05) 머지 직전 1회
- [ ] BE+FE 동시 작업 시 `code-reviewer-backend` 와 **병렬** 호출 (단일 메시지 multiple tool_use) — 본 plan 은 BE 선행 강제로 동시 작업 케이스 부재. 각 PR 단독 리뷰.
- [ ] 컨벤션 위반 0건 (`frontend/.claude/rules/conventions.md` + `architecture.md` + `testing.md`)
  - `any` 금지 / props ≤2 (또는 단일 객체 props 묶기) / 동적 Tailwind 금지 / apiClient 단일 진입점 / 행위 테스트 / 경계만 Mock
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md Phase 2b + Phase 3 FE Pre/Post 정합)
