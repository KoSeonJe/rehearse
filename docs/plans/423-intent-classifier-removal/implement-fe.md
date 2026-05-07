# Implement (Frontend) — IntentClassifier 전면 제거

> **작성자**: frontend agent (Staff Engineer 페르소나 — Claude)
> **답하는 질문**: FE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★ / ★ BE PR 머지 후 진입 (강결합) ★

---

## Phase 0: API Contract 확인 + BE 머지 확인

`tech-spec.md#api-contract` schema 확정 + BE PR 머지 완료 확인. mock 셋업 X — 실제 BE 응답 기준.

- [ ] BE PR 머지 완료 + develop 동기화 (`git checkout develop && git pull`)
- [ ] tech-spec.md `Pre / Post 응답 JSON 비교` 기준 응답 schema 합의 확인
- [ ] dev 환경 BE 가 type 3종 미발행 보장 확인 (실제 호출 1회로 검증)

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 예상 PR | 의존 |
|-------|------|------|--------|------|
| 1 | `types/interview.ts` FollowUpType literal 3종 제거 | `frontend` | #N | Phase 0 |
| 2 | `question-display.tsx` 라벨 매핑 제거 + Integration 테스트 | `frontend` | #N | Phase 1 |
| 3 | `use-answer-flow.ts` skip+presentToUser 분기 제거 + 단위 테스트 | `frontend` | #N | Phase 2 |
| 4 | 빌드 / 린트 / 정적 grep / 회귀 | `frontend` | #N | Phase 3 |

> 단일 PR. Phase = 커밋 단위 분해.

---

## Phase 1: types/interview.ts literal 제거

- **구현**: `frontend` — type 정의 단일 소스 정정 → Phase 2-3 컴파일 에러로 잔여 사용처 노출

### 변경 파일

- `frontend/src/types/interview.ts` (line 262-267) — `FollowUpType` union literal 에서 `'OFF_TOPIC_REDIRECT' | 'CLARIFY_REESTABLISH' | 'GIVE_UP_FALLBACK'` 3종 제거

### 핵심 로직

```ts
// Pre
export type FollowUpType =
  | 'EXPERIENCE'
  | 'CONCEPT'
  | 'RESUME_PLAYGROUND'
  | 'RESUME_INTERROGATION'
  | 'RESUME_WRAP_UP'
  | 'RESUME_OPENER'
  | 'RESUME_HARD_TIMEOUT'
  | 'CONTEXT_BUDGET_EXCEEDED'
  | 'OFF_TOPIC_REDIRECT'
  | 'CLARIFY_REESTABLISH'
  | 'GIVE_UP_FALLBACK';

// Post
export type FollowUpType =
  | 'EXPERIENCE'
  | 'CONCEPT'
  | 'RESUME_PLAYGROUND'
  | 'RESUME_INTERROGATION'
  | 'RESUME_WRAP_UP'
  | 'RESUME_OPENER'
  | 'RESUME_HARD_TIMEOUT'
  | 'CONTEXT_BUDGET_EXCEEDED';
```

`IntentBranch*` 관련 type / interface 잔존 시 동시 삭제 (`grep -E "IntentBranch|intentBranch" frontend/src/types/`).

### 의존

- 선행: Phase 0 (BE 머지 + contract 합의)
- 외부: 없음

### Verification

- `npm run lint -- src/types/interview.ts` 통과
- `npx tsc --noEmit` 시 Phase 2-3 사용처 컴파일 에러 등장 (정상 신호)

### 커밋 메시지

```
refactor(FE): FollowUpType literal에서 의도 분기 3종 제거
```

---

## Phase 2: question-display.tsx 라벨 매핑 제거 + Integration 테스트

- **구현**: `frontend` — 라벨 매핑 entry 정리 + 알 수 없는 type fallback 검증

### 변경 파일

- `frontend/src/components/interview/question-display.tsx` (line 19-24) — 라벨 매핑 객체에서 `OFF_TOPIC_REDIRECT` / `CLARIFY_REESTABLISH` / `GIVE_UP_FALLBACK` entry 제거. fallback (`?? '안내'`) 표현 유지
- `frontend/src/components/interview/question-display.test.tsx` (신규 또는 기존 파일에 추가) — Integration 테스트:
  - 정상 type (`EXPERIENCE`, `RESUME_INTERROGATION` 등) 라벨 표시 검증
  - 알 수 없는 type 수신 시 fallback 라벨 (`'안내'`) 표시 검증

### 핵심 로직

```tsx
// Pre 라벨 매핑
const TYPE_LABEL: Record<FollowUpType, string> = {
  EXPERIENCE: '경험 질문',
  ...
  OFF_TOPIC_REDIRECT: '주제 안내',
  CLARIFY_REESTABLISH: '재질문',
  GIVE_UP_FALLBACK: '도움 안내',
};

// Post
const TYPE_LABEL: Partial<Record<FollowUpType, string>> = {
  EXPERIENCE: '경험 질문',
  ...
  // 의도 분기 3종 entry 제거
};
const label = TYPE_LABEL[type] ?? '안내';  // fallback 유지
```

### 의존

- 선행: Phase 1 (FollowUpType 단일 소스 정정 후 매핑 객체 정합)
- 외부: shadcn primitive (기존 동일)

### Verification

- `npm run test -- src/components/interview/question-display.test.tsx` 통과
- 라벨 fallback 케이스 1건 + 정상 케이스 1건 이상 단언

### 커밋 메시지

```
refactor(FE): question-display 의도 분기 라벨 매핑 제거 + 라벨 fallback 검증 추가
```

---

## Phase 3: use-answer-flow.ts 분기 제거 + 단위 테스트

- **구현**: `frontend` — `skip=true + presentToUser=true` 동시 분기 제거. AI 자체 skip (`skip=true + presentToUser=false`) 만 잔존

### 변경 파일

- `frontend/src/hooks/use-answer-flow.ts` (line 359 근방) — 의도 분기 if/branch 제거. 주석 정리. AI 자체 skip 분기는 유지
- `frontend/src/hooks/use-answer-flow.test.ts` — 의도 분기 fixture 제거. AI 자체 skip 케이스 단언 유지

### 핵심 로직

```ts
// Pre
if (response.skip && response.presentToUser) {
  // 의도 분기 — 사용자에게 안내 표시 + 다음 답변 대기
  showIntentBranchUI(response);
  return;
}
if (response.skip && !response.presentToUser) {
  // AI 자체 skip — 다음 질문 자동 진행
  ...
}

// Post
if (response.skip && !response.presentToUser) {
  // AI 자체 skip — 다음 질문 자동 진행
  ...
}
// 의도 분기 case 제거. response.skip + presentToUser 동시 = 발생 안 함 (BE 보장)
```

### 의존

- 선행: Phase 2 (라벨 매핑 정합 후 훅 분기 제거)
- 외부: TanStack Query (기존 동일)

### Verification

- `npm run test -- src/hooks/use-answer-flow.test.ts` 통과
- AI 자체 skip 케이스 단언 유지 확인 (의도 분기 fixture 만 제거, AI skip fixture 보존)

### 커밋 메시지

```
refactor(FE): use-answer-flow 의도 분기 제거 + 단위 테스트 정리
```

---

## Phase 4: 빌드 / 린트 / 정적 grep / 회귀

- **구현**: `frontend` — 전체 빌드 + 정적 검증 + dev 환경 회귀

### 변경 파일

없음 (검증 단계). 추가 파편 발견 시 해당 파일에 fix.

### Verification

- [ ] `npm run lint` 통과
- [ ] `npm run build` (`tsc -b && vite build`) 통과
- [ ] `npm run test` 전체 통과
- [ ] **정적 검증**: `grep -rEn "OFF_TOPIC_REDIRECT|CLARIFY_REESTABLISH|GIVE_UP_FALLBACK|IntentBranch|intentBranch" frontend/src` → **0건**
- [ ] dev 환경 회귀 (BE 머지 + FE 머지 후) — Resume + Standard 트랙 각 1건 정상 진행 확인 (수동 검수 5건 합격 라인은 BE Phase 4 에서 이미 통과 가정)

### 커밋 메시지

```
chore(FE): IntentClassifier 제거 후 빌드 + 정적 grep 검증
```

(Phase 4 가 검증만이고 변경 파일 0건이면 별도 커밋 생략 가능 — Phase 1-3 커밋으로 충분)

---

## BE 와 통합 시점

- **BE PR 머지 후** FE 진입 (Phase 0 게이트 강제). branch-pr.md 룰 정합
- 통합 = 별도 단계 없음 (mock 부재 — 처음부터 실제 BE)
- BE 머지 ~ FE 머지 window 동안 FE 라벨 fallback (`?? '안내'`) 으로 unknown type 안전 처리 (tech-spec 위험 섹션)

## 통합 Verification

- [ ] tech-spec.md `Verification > FE` 모든 항목 통과
- [ ] tech-spec.md `Pre / Post 응답 JSON 비교` 기준 실제 BE 응답 정합 확인 (dev 환경)
- [ ] FE 머지 후 회귀 (Resume + Standard 트랙 1건 이상)

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-frontend` 실행 (구현 완료 직후 — 메인 세션 책임)
- [ ] BE 선행 강제 = `code-reviewer-backend` 와 병렬 호출 안 함 (시점 분리)
- [ ] 컨벤션 위반 0건 (`frontend/.claude/rules/conventions.md` + `architecture.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md `Pre / Post State` 섹션 기준)
