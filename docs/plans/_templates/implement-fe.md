# Implement (Frontend) — {제목}

> **작성자**: frontend agent
> **답하는 질문**: FE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★

---

## Phase 0: API Contract 확인 + Mock 셋업

`tech-spec.md#api-contract` 의 schema 확정 확인. 실제 BE 구현 대기 X. mock 으로 진행.

- [ ] Endpoint / schema / error 매핑 합의됨
- [ ] Mock 셋업 (MSW handler / hardcoded fixture / TanStack Query mock client)
- [ ] 타입 정의 (`types/api.ts` 등)

---

## Phase / Step 개요

| Phase | 제목 | 예상 PR | 의존 |
|-------|------|--------|------|
| 1 | API client + 타입 (mock 기반) | #N | Phase 0 |
| 2 | Hook / Store (Zustand / TanStack Query) | #N+1 | Phase 1 |
| 3 | UI 컴포넌트 | #N+2 | Phase 2 |
| 4 | BE 통합 (mock 제거) + E2E | #N+3 | BE 머지 |

> Task 8개+ → `tasks/fe-{NN}-{slug}.md` 분리.

---

## Phase 1: {제목}

### 변경 파일
- `frontend/src/api/xxx.ts`
- `frontend/src/types/xxx.ts`

### 핵심 로직
- 단계별 요약 + 의사코드.

### 의존
- 선행: Phase 0 (contract / mock)
- 외부: (TanStack Query / Zustand 등)

### Verification
- `npm run lint`
- `npm run test -- src/api/xxx.test.ts`
- 컴포넌트 스토리북 / Vitest 동작 확인

### 커밋 메시지
```
feat(FE): xxx API client 추가
```

---

## Phase 2-3: ...

(동일 구조)

---

## Phase 4: BE 통합

- BE 머지 알림 수신 후 시작
- mock 제거 (`MSW handler` / `fixture` 삭제 → 실제 endpoint 연결)
- 환경변수 / API base URL 확인
- E2E 시나리오 양측 통과

### Verification
- [ ] mock 흔적 0건 (`grep -r "MSW\|mock"` 결과 검토)
- [ ] 실제 BE 응답 정상 처리 (200 / 4xx / 5xx)
- [ ] tech-spec.md Verification 통과

## 통합 Verification

- [ ] tech-spec.md Verification 통과
- [ ] BE 통합 후 회귀 체크
