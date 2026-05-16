# Implement (Frontend) — Question Pool Admin

> **작성자**: frontend agent
> **답하는 질문**: FE 어떤 순서로 실행?
> **승인 게이트**: ★ 사용자 명시 승인 후 시작 ★

---

## Phase 0: API Contract 확인

`tech-spec.md#api-contract` 의 schema 확정 확인. 실제 BE 구현 전에도 타입/화면은 contract 기준으로 진행 가능하다.

- [ ] Endpoint / schema / error 매핑 합의됨
- [ ] 인증 헤더는 `X-Admin-Password`로 합의됨
- [ ] `referenceType`은 현 API에 포함하지 않는 데 합의됨

미합의 → 즉시 STOP. tech-spec 갱신 + 사용자 승인 재요청.

## Phase / Step 개요

| Phase | 제목 | 구현 | 의존 |
|-------|------|------|------|
| 1 | 타입 + Hook | `frontend` | Phase 0 |
| 2 | Page UI | `frontend` | Phase 1 |
| 3 | Route + a11y smoke | `frontend` | Phase 2 |
| 4 | Interaction 테스트 | `frontend` | Phase 2 |
| 5 | Verification | `frontend` | Phase 1-4 |

## Phase 1: 타입 + Hook

- **구현**: API contract에 맞춘 타입과 TanStack Query hook 추가.

### 변경 파일

- Create: `frontend/src/types/question-pool.ts`
- Create: `frontend/src/hooks/use-admin-question-pool.ts`

### 핵심 로직

- 타입:
  - `AdminQuestionPoolItem`
  - `AdminQuestionPoolListResponse`
  - `AdminQuestionPoolFilters`
  - `CreateQuestionPoolRequest`
- `useAdminQuestionPools(filters, page, size)`:
  - `URLSearchParams`로 blank가 아닌 필터만 포함
  - `X-Admin-Password`는 `sessionStorage.getItem('admin-password') ?? ''`
- `useCreateAdminQuestionPool()`:
  - POST `/api/v1/admin/question-pools`
  - success 시 `queryClient.invalidateQueries({ queryKey: ['admin-question-pools'] })`

### Verification

- 타입 체크는 FE 테스트/빌드에서 확인한다.

## Phase 2: Page UI

- **구현**: 운영자가 반복 사용할 수 있는 조밀한 관리 화면.

### 변경 파일

- Create: `frontend/src/pages/admin-question-pool-page.tsx`

### 핵심 로직

- State:
  - `page`, `draftFilters`, `appliedFilters`
  - create dialog open state
  - create form state
- 목록:
  - loading: spinner
  - empty: "조건에 맞는 질문이 없습니다"
  - desktop: table
  - mobile: card list
- 필터:
  - cacheKey input
  - category input
  - isActive select: 전체/활성/비활성
  - keyword input
  - 검색 시 page를 0으로 reset
  - 초기화 시 filters reset
- 생성 dialog:
  - cacheKey, content 필수
  - ttsContent, category, bestAnswer optional
  - mutation pending 중 submit disabled
  - success 시 dialog close + form reset

### Verification

- 로딩/빈 상태/데이터 상태를 수동 및 테스트로 확인한다.

## Phase 3: Route + a11y smoke

- **구현**: 기존 비밀번호 보호 라우트 아래 신규 페이지 연결.

### 변경 파일

- Modify: `frontend/src/app.tsx`
- Modify: `frontend/tests/a11y/pages.spec.tsx`

### 핵심 로직

- `App`:
  - import `AdminQuestionPoolPage`
  - `<Route path="/admin/question-pool" element={<AdminQuestionPoolPage />} />`
- a11y:
  - `useAdminQuestionPools`, `useCreateAdminQuestionPool` mock 추가
  - `admin-question-pool` smoke case 추가

### Verification

- `npm run test -- tests/a11y/pages.spec.tsx`

## Phase 4: Interaction 테스트

- **구현**: 페이지의 핵심 사용자 동작을 RTL로 검증.

### 변경 파일

- Create: `frontend/src/pages/__tests__/admin-question-pool-page.test.tsx`

### 핵심 로직

- hook mock 기반 테스트:
  - 데이터 row 렌더링
  - 검색 버튼 클릭 시 필터 적용 UI state 확인
  - 새 질문 dialog 열림
  - cacheKey/content 입력 후 생성 mutation 호출
  - 생성 성공 callback으로 dialog close 가능

### Verification

- `npm run test -- src/pages/__tests__/admin-question-pool-page.test.tsx`

## Phase 5: Frontend Verification

- [ ] `cd frontend && npm run test -- src/pages/__tests__/admin-question-pool-page.test.tsx`
- [ ] `cd frontend && npm run test -- tests/a11y/pages.spec.tsx`
- [ ] `cd frontend && npm run lint`
- [ ] 필요 시 `cd frontend && npm run build`

## Phase 6: 세부 주제 선택 UX

> **추가 승인**: 사용자가 2026-05-17에 "응 이렇게 해줘"로 승인.

- **구현**: `category` 필드는 DB/API 필드명을 유지하되, 관리자 화면에서는 `세부 주제`로 표시하고 생성 폼에서는 추천 옵션 드롭다운과 `기타` 직접 입력을 제공한다.

### 변경 파일

- Modify: `frontend/src/pages/admin-question-pool-page.tsx`
- Modify: `frontend/src/pages/__tests__/admin-question-pool-page.test.tsx`

### 핵심 로직

- 검색 필터/목록 테이블/카드/생성 폼 라벨:
  - `카테고리`, `추가할 카테고리` 표시를 `세부 주제`로 변경한다.
- 생성 폼:
  - 면접 유형/기술스택 선택값에서 추천 세부 주제 목록을 계산한다.
  - `CS_FUNDAMENTAL`: `자료구조/알고리즘`, `운영체제`, `네트워크`, `데이터베이스`
  - `BEHAVIORAL`: `협업`, `갈등 해결`, `프로젝트 경험`, `성장 경험`
  - 기술스택 기반 유형은 기술스택별 대표 주제를 노출한다.
  - 마지막 옵션은 `기타`로 두고, `기타` 선택 시 직접 입력 input을 노출한다.
  - submit 시 선택된 세부 주제 또는 직접 입력값을 기존 `category` 필드로 전송한다.
  - 세부 주제가 비어도 기존 API contract처럼 optional로 전송 가능하게 유지한다.
- 테스트:
  - 추천 세부 주제를 선택하면 mutation의 `category`에 선택값이 들어간다.
  - `기타` 선택 후 직접 입력하면 mutation의 `category`에 직접 입력값이 들어간다.
  - 검색 필터 라벨은 `세부 주제`로 접근 가능하다.

### Verification

- [ ] `cd frontend && npm run test -- src/pages/__tests__/admin-question-pool-page.test.tsx`
- [ ] `cd frontend && npm run test -- tests/a11y/pages.spec.tsx`
- [ ] `cd frontend && npm run lint`
- [ ] 필요 시 `cd frontend && npm run build`

## BE 와 통합 시점

- BE endpoint가 contract와 다르게 구현되면 FE 수정 전 `tech-spec.md` 갱신 + 사용자 승인.
- 실제 backend와 통합할 때 `/admin/question-pool`에서 목록 조회와 단건 생성이 200으로 동작하는지 확인한다.

## 리뷰 게이트 (MANDATORY)

- [ ] 구현 완료 직후 frontend review 수행
- [ ] 컨벤션 위반 0건 (`frontend/AGENTS.md` + frontend rules)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치
