# Implement (Frontend) — 이력서 면접 트랙 단순화 (QuestionType enum 갱신)

> **작성자**: frontend agent
> **답하는 질문**: FE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 + BE Phase 2 머지 완료 후 시작 ★
> **출처**: `tech-spec.md` §API Contract L437-446 (FE 영향 분리 작업 필수)

---

## Phase 0: API Contract 확인

`tech-spec.md` §API Contract 확정. **BE Phase 2 선행 머지 필수** (강결합 = BE 응답 schema source of truth).

- [x] Endpoint 변경 없음 (`POST /api/v1/interviews/{id}/follow-up`)
- [x] `QuestionType` literal union — 5 → 6 멤버 (`RESUME_PLAYGROUND`/`RESUME_INTERROGATION` 제거 + `RESUME_OPENER` 유지 + `RESUME_MAIN`/`RESUME_FOLLOWUP` 추가 + 표준 트랙 3 유지)
- [x] `selectedAnswerFeedbackPerspective` 송신 필드 FE 송신 부재 grep 확인 (`FollowUpRequest` payload 생성 코드)
- [x] mock 미적용 — BE 머지 완료 후 시작 (강결합 BE 선행)

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 예상 PR | 의존 |
|-------|------|------|--------|------|
| 1 | QuestionType 갱신 + label + 분기 + 테스트 fixture | `frontend` | #N | BE Phase 2 머지 |

> 단일 Phase 5 task inline. tasks/ 분리 임계 (8 task / 50줄+) 미초과.

---

## Phase 1: QuestionType 갱신 + 분기 / Label / 테스트 fixture

- **구현**: `frontend` — BE QuestionType enum 변경에 맞춰 FE union 타입 + 유틸 분기 + UI label + 테스트 fixture 갱신

### 변경 파일

- `frontend/src/types/interview.ts:63-64` — `QuestionType` literal union 갱신
  ```ts
  // Before
  type QuestionType = 'TECH_MAIN' | 'TECH_FOLLOWUP' | 'BEHAVIORAL_MAIN' | 'BEHAVIORAL_FOLLOWUP' | 'RESUME_OPENER' | 'RESUME_PLAYGROUND' | 'RESUME_INTERROGATION';
  // After
  type QuestionType = 'TECH_MAIN' | 'TECH_FOLLOWUP' | 'BEHAVIORAL_MAIN' | 'BEHAVIORAL_FOLLOWUP' | 'RESUME_OPENER' | 'RESUME_MAIN' | 'RESUME_FOLLOWUP';
  ```
- `frontend/src/utils/question-type.ts:6` — `isMain` / `isFollowup` 분기 갱신
  - `isMain`: `RESUME_MAIN` 추가
  - `isFollowup`: `RESUME_FOLLOWUP` 추가
- `frontend/src/utils/__tests__/question-type.test.ts:30,31,51` — 테스트 fixture 갱신 (RESUME_MAIN main 분류, RESUME_FOLLOWUP followup 분류)
- `frontend/src/components/feedback/__tests__/content-tab.test.tsx:56,125,135,150` — 테스트 fixture 갱신
- `frontend/src/components/feedback/feedback-panel.tsx:14-15` — label map 갱신
  - `RESUME_MAIN` 한국어 label: "이력서 질문" (예시 — Autonomous Designer Mode 메모리 자율 결정)
  - `RESUME_FOLLOWUP` 한국어 label: "이력서 꼬리질문" (예시 — 자율 결정)
- `FollowUpRequest` payload 생성 코드 grep — `selectedAnswerFeedbackPerspective` 송신 발견 시 제거 (grep 0 예상, tech-spec 확인됨)

### 핵심 로직

```ts
// utils/question-type.ts
export function isMain(type: QuestionType): boolean {
  return type === 'TECH_MAIN' || type === 'BEHAVIORAL_MAIN' || type === 'RESUME_MAIN';
}

export function isFollowup(type: QuestionType): boolean {
  return type === 'TECH_FOLLOWUP' || type === 'BEHAVIORAL_FOLLOWUP' || type === 'RESUME_FOLLOWUP';
}

// components/feedback/feedback-panel.tsx
const QUESTION_TYPE_LABEL: Record<QuestionType, string> = {
  TECH_MAIN: '기술 질문',
  TECH_FOLLOWUP: '꼬리질문',
  BEHAVIORAL_MAIN: '행동 질문',
  BEHAVIORAL_FOLLOWUP: '행동 꼬리질문',
  RESUME_OPENER: '이력서 오프너',
  RESUME_MAIN: '이력서 질문',       // NEW
  RESUME_FOLLOWUP: '이력서 꼬리질문', // NEW
};
```

### 의존
- 선행: BE Phase 2 머지 완료 (응답 schema 신규 enum literal 도달 source 확보)
- 외부: 없음

### Verification
- `npm run lint` — `any` 부재, 신규 union 멤버 type-safe
- `npm run build` — `tsc -b` strict 통과
- `npm run test -- src/utils/__tests__/question-type.test.ts` — 분기 회귀
- `npm run test -- src/components/feedback/__tests__/content-tab.test.tsx` — 컴포넌트 fixture 회귀
- 수동: dev 환경 신규 이력서 면접 진입 → 피드백 패널에서 `RESUME_MAIN` / `RESUME_FOLLOWUP` row label 정상 노출
- 통과 기준: 모든 테스트 green + grep `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` / `selectedAnswerFeedbackPerspective` 잔존 0

### 커밋 메시지

```
refactor(FE): QuestionType 이력서 트랙 enum 갱신 (RESUME_MAIN/FOLLOWUP)
```

---

## BE 통합 시점

- BE Phase 2 머지 직후 FE PR 시작 (강결합 = BE 선행 강제)
- mock 미사용 — BE 응답 schema 가 source of truth
- 통합 검증: BE Phase 2 머지된 dev 환경에서 FE 빌드 + 신규 enum literal 응답 정상 처리

## 통합 Verification

- [ ] tech-spec.md §API Contract FE 영향 9건 (L437-446) 모두 반영
- [ ] grep `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` 잔존 0
- [ ] grep `selectedAnswerFeedbackPerspective` 잔존 0
- [ ] dev 환경 수동 검증 = 신규 이력서 면접 / 피드백 패널 label 정상

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-frontend` 실행 (구현 완료 직후 — 메인 세션 책임)
- [ ] `code-reviewer-backend` 와 **병렬** 호출 (BE+FE 동시 작업, 단일 메시지 multiple tool_use)
- [ ] 컨벤션 위반 0건 (`frontend/.claude/rules/conventions.md` + `architecture.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff = tech-spec §Pre/Post State 일치
