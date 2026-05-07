# Implement (Frontend) — Resume 트랙 WRAP_UP 모드 제거 (terminate 신호 + 유니언 정리)

> **작성자**: frontend agent
> **답하는 질문**: FE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★

---

## Phase 0: API Contract 확인 + 선행 의존

`tech-spec.md#api-contract` schema 확정 + 사용자 명시 승인 완료.

- [x] Endpoint = 기존 `POST /api/v1/interviews/{id}/follow-up` (변경 없음)
- [x] Request schema = `FollowUpRequest.terminate: boolean` (default false)
- [x] Response schema 변경 없음 — 종료 케이스 = `followUpExhausted=true` 재사용 (기존 hard timeout 경로 동일)
- [x] Error 매핑 변경 없음

### 선행 의존 (강결합 — BE 선행)

- [ ] **BE PR `[BE] refactor: Resume 트랙 WRAP_UP 모드 제거` 머지 완료 + dev 배포 검증** — tech-spec 강결합 BE 선행 룰. mock 진행 X. 실제 BE dev 응답으로 통합 테스트.
- [ ] BE dev 배포 후 Resume follow-up 응답에서 `terminate` 필드 무시 정상 동작 확인 (기존 동작 회귀)

미머지 → STOP. BE 머지 + dev 배포 후 진입.

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 예상 PR | 의존 |
|-------|------|------|--------|------|
| 1 | 타입 정리 — QuestionType 유니언 + FollowUpRequest terminate | `frontend` | #M | Phase 0 (BE 머지) |
| 2 | 잔여 시간 모니터링 hook (interview-store 기반) | `frontend` | #M (동일 PR) | Phase 1 |
| 3 | follow-up payload terminate 신호 + 종료 페이즈 분기 | `frontend` | #M (동일 PR) | Phase 2 |
| 4 | 통합 테스트 (msw + RTL) + 회귀 | `frontend` | #M (동일 PR) | Phase 3 |

> 단일 PR `[FE] feat: Resume 인터뷰 종료 신호 + WRAP_UP 유니언 정리` — 4 Phase 작업 단위 커밋 분할 후 1 PR 묶음.

---

## Phase 1: 타입 정리

- **구현**: `frontend` — Resume 종료 모드 타입 산출 제거 + Request 타입에 `terminate` 추가

### 변경 파일

- `frontend/src/types/interview.ts` — `QuestionType` 유니언에서 `'RESUME_WRAP_UP'` 제거 (5종으로 축소). 다른 RESUME_* 유지.
- `frontend/src/types/interview.ts` (또는 follow-up Request 타입 위치) — `FollowUpRequest` 타입 인터페이스에 `terminate?: boolean` 추가 (선택, default false).

> 실제 path / 인터페이스명은 진입 시점 grep `FollowUpRequest|QuestionType` frontend/src/types 로 재확정.

### 핵심 로직

1. 유니언에서 `'RESUME_WRAP_UP'` 제거 → 사용처 TS strict 컴파일 에러 → grep 동시 정리.
2. `FollowUpRequest` 타입 확장 — `terminate?: boolean` (BE 호환 위해 optional / 미전송 = false BE 기본값).

### 의존

- 선행: Phase 0 (BE 머지)
- 외부: 없음

### Verification

- [ ] `npm run lint`
- [ ] `tsc --noEmit` (vite build 단계 자동) 통과
- [ ] grep `RESUME_WRAP_UP` frontend/src → 0건

### 커밋 메시지

```
refactor(FE): QuestionType 유니언에서 RESUME_WRAP_UP 제거 + FollowUpRequest.terminate 타입 추가
```

---

## Phase 2: 잔여 시간 모니터링 hook

- **구현**: `frontend` — 인터뷰 시작 시각 기준 elapsed / remaining 계산 + 답변 제출 시점 잔여 ≤ 0 판정 헬퍼

### 변경 파일

- `frontend/src/hooks/use-interview-elapsed.ts` (또는 기존 hook 확장) — 신규 또는 갱신:
  - 입력: 인터뷰 시작 시각 (`interview-store` 의 `startedAt` 등 — 진입 시점 store 구조 grep 후 확정)
  - 출력: `{ elapsedSeconds: number, remainingSeconds: number, isOverdue: boolean }`
  - 구현: `Date.now()` 차이 계산. arbitrary Tailwind / `setTimeout` 기반 polling 회피 — `useEffect` + `setInterval` cleanup + `AbortController` 패턴.
- `frontend/src/stores/interview-store.ts` (또는 동등 위치) — 인터뷰 시작 시각 보유 여부 검토. 부재 시 `startedAt: number | null` 추가 + 셋업 시 `Date.now()` 기록.

> 실제 store / hook 구조는 진입 시점 `frontend/src/hooks/use-interview-session.ts` Read + grep 후 확정.

### 핵심 로직

```
useInterviewElapsed(startedAt, durationMinutes):
  elapsedSeconds = (Date.now() - startedAt) / 1000
  remainingSeconds = durationMinutes * 60 - elapsedSeconds
  isOverdue = remainingSeconds <= 0
  // useEffect setInterval 1s tick
  return { elapsedSeconds, remainingSeconds, isOverdue }
```

### 의존

- 선행: Phase 1 (타입)
- 외부: 없음 (TanStack Query / Zustand 기존)

### Verification

- [ ] Vitest unit: `useInterviewElapsed` hook — startedAt 기준 elapsed / remaining / isOverdue 계산 검증 (vi.useFakeTimers)
- [ ] cleanup 검증 — unmount 시 interval 해제 (memory leak 방지)

### 커밋 메시지

```
feat(FE): 인터뷰 잔여 시간 모니터링 hook 추가
```

---

## Phase 3: follow-up payload terminate 신호 + 종료 페이즈 분기

- **구현**: `frontend` — 답변 완료 시점 잔여 ≤ 0 + `terminate: true` 동봉 / `followUpExhausted=true` 응답 시 종료 페이즈 진입

### 변경 파일

- `frontend/src/hooks/use-interview-session.ts` — Resume 분기 (`:183` 인근) 답변 제출 mutation 호출부:
  - 답변 완료 시점에 `useInterviewElapsed().isOverdue` 판정
  - true → request payload `terminate: true` 동봉 / false → `terminate: false` (또는 미포함 — BE default false)
  - 답변 입력 도중 잔여 ≤ 0 도달해도 끊지 않음 — 현재 답변 완료 후 다음 제출 시점에만 신호 (tech-spec 정책)
- `frontend/src/api/interview.ts` (또는 follow-up API client 위치) — request body 직렬화에 `terminate` 필드 포함 보장
- `frontend/src/components/interview/...` (종료 페이즈 분기 컴포넌트) — `followUpExhausted=true` 수신 시 종료 UI 진입. 기존 hard timeout 경로 그대로 활용 가능 시 추가 변경 0.

> 종료 페이즈 분기 위치는 진입 시점 grep `followUpExhausted` frontend/src 로 확정. 기존 분기 재활용이 원칙.

### 핵심 로직

```
onSubmitAnswer(answerText):
  const { isOverdue } = useInterviewElapsed(startedAt, durationMinutes)
  await followUpMutation.mutate({
    questionSetId,
    questionContent,
    answerText,
    nonVerbalSummary,
    previousExchanges,
    terminate: isOverdue,  // 답변 완료 시점 판정
  })
  // response.followUpExhausted === true → 종료 페이즈 (기존 분기 재활용)
```

### 의존

- 선행: Phase 2 (hook)
- 외부: BE dev 배포 (Phase 0)

### Verification

- [ ] Vitest integration (RTL + msw): 잔여 ≤ 0 + 답변 제출 시 request body 에 `terminate: true` 포함 (msw handler request assert)
- [ ] Vitest integration: 잔여 > 0 + 답변 제출 시 `terminate: false` (또는 미포함)
- [ ] Vitest integration: `followUpExhausted=true` 응답 → 종료 UI 페이즈 노출 (기존 동작 회귀)
- [ ] Vitest integration: 잔여 ≤ 0 도중 답변 입력 중 → terminate 미전송 / 답변 완료 후 제출 시점에만 전송

### 커밋 메시지

```
feat(FE): Resume follow-up 답변 제출 시 잔여 시간 ≤ 0 = terminate 신호 동봉
```

---

## Phase 4: 통합 테스트 + 회귀

- **구현**: `frontend` — Resume 페이지 전체 시나리오 + 정적 grep + 회귀

### 변경 파일

- `frontend/src/hooks/__tests__/use-interview-session.test.ts` (또는 동등) — Resume 분기 시나리오 추가
- `frontend/src/pages/__tests__/interview-page.test.tsx` (있다면) — Resume 인터뷰 시작 → 다중 턴 → duration 초과 → 답변 제출 → 종료 페이즈 시나리오 1개

### 핵심 로직

- testing.md 정책: msw 핸들러로 BE 응답 mock (실제 dev BE 호출은 통합 단계 별도 / 단위·통합 테스트는 msw)
- userEvent.click + screen.findByRole 표준 패턴
- `getByTestId` 남발 회피 — `getByRole` / `getByLabelText` 우선

### 의존

- 선행: Phase 3
- 외부: BE dev 배포 응답 (E2E 1회 수동 검증)

### Verification

- [ ] `npm run lint`
- [ ] `npm run test` 전체 그린
- [ ] grep `RESUME_WRAP_UP|wrap_up|RESUME_HARD_TIMEOUT 외 wrap_up` frontend/src → 0건 (RESUME_HARD_TIMEOUT 응답 type 자체는 BE 가 보내지 않음 / FE 의 의존 타입 위치 점검)
- [ ] BE dev 배포 후 수동 E2E 1회: Resume 시작 → duration 초과 후 답변 제출 → followUpExhausted=true 수신 → 종료 페이즈 정상 진입

### 커밋 메시지

```
test(FE): Resume terminate 신호 / 종료 페이즈 통합 테스트 추가
```

---

## Phase 4: BE 통합 (재확인)

- BE 머지 알림 수신 후 시작 (Phase 0 게이트)
- mock 제거 — 본 plan 은 BE 선행 강결합으로 mock 진행 단계 자체가 없음. msw 는 단위·통합 테스트 한정 사용.
- 환경변수 / API base URL 확인 (`VITE_API_URL` dev)
- E2E 시나리오 양측 통과

### Verification

- [ ] mock 흔적 = 단위·통합 테스트 영역 한정 (`src/mocks/handlers.ts` 외 production 코드 mock 0)
- [ ] 실제 BE 응답 정상 처리 (200 / 4xx / 5xx)
- [ ] tech-spec.md Verification 통과

## 통합 Verification

- [ ] tech-spec.md Verification 섹션 (FE Integration 3항목 + 정적 grep 1항목 + 빌드 2항목) 통과
- [ ] BE 통합 후 회귀 (Resume 정상 다중 턴 / 종료 페이즈 / 다른 트랙 영향 0)

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-frontend` 실행 (구현 완료 직후 — 메인 세션 책임)
- [ ] BE+FE 동시 작업 시 `code-reviewer-backend` 와 **병렬** 호출 (단일 메시지 multiple tool_use). 본 plan 은 강결합 BE 선행 → BE 리뷰 / 머지 후 FE 진입 → FE 리뷰는 단독 실행이 정상.
- [ ] 컨벤션 위반 0건 (`frontend/.claude/rules/conventions.md` + `architecture.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec `## Pre / Post State` FE 항목 기준)
