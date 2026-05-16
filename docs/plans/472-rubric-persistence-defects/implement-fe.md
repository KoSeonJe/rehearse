# Implement (Frontend) — rubric 채점 결과 적재 결함 정합화

> **작성자**: frontend agent
> **답하는 질문**: FE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★

---

## Phase 0: API Contract 확인 + Mock 셋업

본 plan = API contract **변경 없음**. `TimestampFeedbackResponse.questionType` 필드 이미 BE wire 노출 중.

- [x] Endpoint / schema / error 매핑 = 변경 없음
- [x] Mock 셋업 불필요 (기존 응답 schema 그대로 사용)
- [x] 타입 정의 = 기존 `frontend/src/types/interview.ts` 의 `TimestampFeedback` 또는 `Interview` 관련 타입에 `questionType` 필드 존재 여부 확인 (Phase 1 진입 시 첫 점검)

→ BE 머지 대기 X. BE Phase 1/2 와 병렬 진행 가능.

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 예상 PR | 의존 |
|-------|------|------|--------|------|
| 1 | `ContentTab` questionType prop 추가 + OPENER 분기 + 호출부 수정 + 테스트 | `frontend` | PR-C | Phase 0 |

> Task 1개 — 단일 파일 유지.

---

## Phase 1: ContentTab questionType prop 추가 + OPENER 분기

- **구현**: `frontend` — `ContentTab` 컴포넌트에 `questionType` prop 추가. `RESUME_OPENER` = "안내" 카드 / 그 외 + null = 기존 `FALLBACK_COPY` (결함성 안내). 호출부 수정 + 회귀 테스트.

### 변경 파일

- `frontend/src/components/feedback/content-tab.tsx`
  - line 3-5 `ContentTabProps` interface 에 `questionType: string | null` 추가
  - line 17-20 `FALLBACK_COPY` 의미 재정의 (문구 그대로 유지 = 결함성 케이스 안내)
  - 신규 상수 `OPENER_COPY = { title: '안내', emptyMessage: '이 단계는 채점 대상이 아닙니다.', secondary: '면접 도입 단계 답변은 점수 채점에 사용되지 않습니다.' }`
  - line 37-55 `ContentTab` 본문에 `questionType === 'RESUME_OPENER'` early return 분기 추가
- 호출부 수정 — `ContentTab` 사용처 grep:
  - `grep -rn "ContentTab" frontend/src/`
  - 각 호출부에서 `questionType` prop 전달 추가 (timestamp feedback 응답 객체에서 매핑)
- `frontend/src/types/interview.ts` (or 해당 응답 타입 정의 파일)
  - `TimestampFeedback` (또는 동급 타입) 에 `questionType: string | null` 필드 존재 확인 / 누락 시 추가
- `frontend/src/components/feedback/__tests__/content-tab.test.tsx` (신규)
  - 4 시나리오 Integration 테스트

### 핵심 로직

```tsx
interface ContentTabProps {
  technicalFeedback: TechnicalFeedback | null
  questionType: string | null
}

const OPENER_COPY = {
  title: '안내',
  emptyMessage: '이 단계는 채점 대상이 아닙니다.',
  secondary: '면접 도입 단계 답변은 점수 채점에 사용되지 않습니다.',
} as const

const ContentTab = ({ technicalFeedback, questionType }: ContentTabProps) => {
  if (questionType === 'RESUME_OPENER') {
    return (
      <div className="px-6 py-10 text-center">
        <p className="text-[15px] font-bold text-gray-900">{OPENER_COPY.emptyMessage}</p>
        <p className="mt-2 text-[13px] leading-relaxed text-gray-400">{OPENER_COPY.secondary}</p>
      </div>
    )
  }
  // 기존 로직 그대로 (FALLBACK_COPY = 결함성 안내로 의미 재정의)
  ...
}
```

### 의존

- 선행: Phase 0 (contract = 변경 없음)
- 외부: 없음 (TanStack Query / Zustand 무관 — pure presentational 변경)

### Verification

- `npm run lint`
- `npm run build`
- `npm run test -- src/components/feedback/__tests__/content-tab.test.tsx`
- 신규 테스트 (`frontend/.claude/rules/testing.md` Integration):
  - 시나리오 (a) `questionType='RESUME_OPENER'` + `technicalFeedback=null` → "안내" + "이 단계는 채점 대상이 아닙니다." + 보조 카피 3종 노출
  - 시나리오 (b) `questionType='RESUME_PLAYGROUND'` + `technicalFeedback=null` → 기존 `FALLBACK_COPY` ("해당 턴은 평가 대상이 아닙니다") 노출 (결함성 안내)
  - 시나리오 (c) `questionType='RESUME_INTERROGATION'` + `technicalFeedback` 정상 (dimensions 있음) → 기존 dimension 카드 회귀
  - 시나리오 (d) `questionType='TECH_MAIN'` + `technicalFeedback` 정상 → 기존 TECHNICAL 카테고리 카드 회귀
- 수동 확인: dev 환경 RESUME 인터뷰 진입 → OPENER turn 화면 = "안내" 카드. 스크린샷 1장 PR 본문 첨부
- 통과 기준: lint + build + 4 시나리오 green + 수동 스크린샷

### 커밋 메시지

```
fix(FE): ContentTab 에 questionType 분기 추가, OPENER 안내 카드 분리
```

---

## Phase 2: BE 통합

본 plan = BE wire 변경 없음 (`questionType` 이미 노출). mock 제거 단계 불필요.

- BE Phase 1 머지 후 → INTERROGATION 4차원 채점 row 적재 → 기존 카테고리 카드가 4차원으로 노출되는지 dev 환경 확인 (회귀 체크 only)
- BE Phase 2 머지 후 → 비언어 점수 적재 결함 fix 진행 시 비언어 화면 회귀 체크

### Verification

- [ ] BE Phase 1 머지 후 dev RESUME 인터뷰 → INTERROGATION turn 화면 = TECHNICAL 카테고리 4차원 (technical_depth / reasoning_communication / factual_consistency / chain_depth) 카드 노출
- [ ] BE Phase 2 머지 + 비언어 fix 후 dev RESUME 인터뷰 → 비언어 점수 4항목 (눈맞춤 / 떨림 / 침착함 / 유창함) 화면 노출

## 통합 Verification

- [ ] tech-spec.md `## Verification` 통과
- [ ] BE 통합 후 회귀 체크 (위 Phase 2 항목)

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-frontend` 실행 (구현 완료 직후 — 메인 세션 책임)
- [ ] BE+FE 동시 작업 → `code-reviewer-backend` 와 **병렬** 호출 (단일 메시지 multiple tool_use)
- [ ] 컨벤션 위반 0건 (`frontend/.claude/rules/conventions.md` + `architecture.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec `## Pre / Post State`)
