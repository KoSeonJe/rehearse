# Implement (Frontend) — RESUME 트랙 채점 perspective 별 라벨 라우팅 정상화

> **작성자**: frontend agent
> **답하는 질문**: FE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★

---

## Phase 0: API Contract 확인

`tech-spec.md#api-contract` 응답 schema 합의 확인. BE 머지 대기 X — 타입 정의 + props 분기 단독 검증 가능.

- [ ] Endpoint 변경 없음 (기존 피드백 조회 endpoint)
- [ ] Response schema 추가: `technicalFeedback.perspective: "TECHNICAL"|"BEHAVIORAL"|"EXPERIENCE"|null`
- [ ] FE 타입: `FeedbackPerspective = 'TECHNICAL' | 'EXPERIENCE' | 'BEHAVIORAL'` union
- [ ] mock fixture 미생성: `frontend/src/mocks/` = dev 폴백 전용 (msw 미사용)

미합의 → 즉시 STOP. tech-spec 갱신 + 사용자 승인 재요청.

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 예상 PR | 의존 |
|-------|------|------|--------|------|
| 1 | TechnicalFeedback 타입 + ContentTab perspective 라벨 분기 + Integration 테스트 | `frontend` | #N | Phase 0 |
| 1.5 | 코드리뷰 P1 4건 반영 (union 강타입 + fallback 분기 + RTL 변환) | `frontend` | (Phase 1 PR 동일) | Phase 1 |

> Phase 1 = 본 plan 재진입 전 완료 (브랜치 `feat/436-fe-perspective-routing` 커밋 `0f18ebc` 보존). Phase 1.5 = 재진입 시 추가 커밋.

---

## Phase 1: TechnicalFeedback 타입 확장 + ContentTab perspective 라벨 분기 + 테스트

> 이미 완료. 커밋 `0f18ebc` 보존 — 리뷰 트레이스 가시성 유지.

### 변경 파일 (Phase 1 — 완료)

- `frontend/src/types/interview.ts` — `TechnicalFeedback.perspective: string | null`
- `frontend/src/components/feedback/content-tab.tsx` — perspective 분기 라벨
- `frontend/src/components/feedback/__tests__/content-tab.test.tsx` — 4 케이스

### 커밋 메시지 (Phase 1)

```
feat(FE): 채점 perspective 별 라벨/영역 분기 노출
```

---

## Phase 1.5: 코드리뷰 P1 4건 반영 (재진입)

- **구현**: `frontend` — P1 4건 추가 커밋. amend X (Phase 1 트레이스 보존).

### P1-1: FeedbackPerspective union 강타입 도입

```ts
// types/interview.ts
export type FeedbackPerspective = 'TECHNICAL' | 'EXPERIENCE' | 'BEHAVIORAL'

export interface TechnicalFeedback {
  perspective: FeedbackPerspective | null   // 강타입
  rubricId: string
  levelFlag: string | null
  dimensions: TechnicalDimensionFeedback[]
}
```

### P1-2 + P1-4: fallback 메시지 perspective 별 분기

```ts
// content-tab.tsx
const PERSPECTIVE_LABEL: Record<FeedbackPerspective, string> = {
  TECHNICAL: '기술 피드백',
  EXPERIENCE: '경험 평가',
  BEHAVIORAL: '행동 피드백',
}

function resolveLabel(perspective: FeedbackPerspective | null): { title: string; emptyMessage: string } {
  switch (perspective) {
    case 'TECHNICAL':
      return { title: '기술 피드백', emptyMessage: '기술 피드백은 아직 준비 중입니다.' }
    case 'EXPERIENCE':
      return { title: '경험 평가', emptyMessage: '경험 평가는 아직 준비 중입니다.' }
    case 'BEHAVIORAL':
    default:
      return { title: '기술 피드백', emptyMessage: '해당 턴은 평가 대상이 아닙니다.' }
  }
}
```

### P1-3: 테스트 RTL 변환

- `renderToStaticMarkup` 패턴 → `screen.getByText({ exact: true })` / `queryByText`
- 4 케이스 (TECHNICAL / EXPERIENCE / BEHAVIORAL / null) 모두 RTL 단언

### 변경 파일 (Phase 1.5)

- `frontend/src/types/interview.ts` — union 타입 추가, `TechnicalFeedback.perspective` 강타입화
- `frontend/src/components/feedback/content-tab.tsx` — fallback 분기 + PERSPECTIVE_LABEL Record
- `frontend/src/components/feedback/__tests__/content-tab.test.tsx` — RTL 변환

### 의존

- 선행: Phase 1 (커밋 `0f18ebc`)

### Verification

- `npm run lint`
- `npm run test -- src/components/feedback/__tests__/content-tab.test.tsx` — 4 케이스 RTL 단언
- `npm run build` — TS strict (union 타입 exhaustiveness)

### 커밋 메시지

```
refactor(FE): perspective 라벨 분기 코드리뷰 P1 반영
```

---

## BE 통합 시점

- BE prod 배포 + 응답에 `perspective` 필드 노출 확인 → FE 머지 (tech-spec backward compat 룰).
- **머지 순서 필수**: BE 우선. FE 단독 머지 시 perspective undefined → 모든 turn fallback (시각 회귀 강함).

## 통합 Verification

- [ ] tech-spec.md Verification 통과
- [ ] BE 머지 후 dev 환경 RESUME PLAYGROUND turn → "경험 평가" 라벨 노출 확인
- [ ] BE 머지 후 dev 환경 STANDARD TECH_MAIN turn → "기술 피드백" 라벨 회귀 없음

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-frontend` 실행 (Phase 1.5 직후)
- [ ] BE+FE 동시 작업 → `code-reviewer-backend` 와 **병렬** 호출 (단일 메시지 multiple tool_use)
- [ ] 컨벤션 위반 0건 (`frontend/.claude/rules/conventions.md` + `architecture.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md Pre/Post 섹션)
