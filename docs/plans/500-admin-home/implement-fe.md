# Implement (Frontend) — Admin Home

> **작성자**: frontend agent
> **답하는 질문**: FE 어떤 순서로 실행?
> **승인 게이트**: 사용자 명시 승인 완료 후 시작

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 의존 |
|-------|------|------|------|
| 1 | 실패 테스트 작성 | `frontend` | 없음 |
| 2 | `/admin` 페이지 구현 | `frontend` | Phase 1 |
| 3 | route/a11y 연결 | `frontend` | Phase 2 |
| 4 | 검증 | `frontend` | Phase 3 |

## Phase 1: 실패 테스트 작성

- **구현**: `frontend` — `/admin` 런처가 노출해야 하는 링크 동작을 테스트로 고정한다.

### 변경 파일
- `frontend/src/pages/__tests__/admin-home-page.test.tsx`

### Verification
- `cd frontend && npm run test -- src/pages/__tests__/admin-home-page.test.tsx`
- 기대: `AdminHomePage` 미구현 import 오류 또는 링크 미존재 실패

## Phase 2: `/admin` 페이지 구현

- **구현**: `frontend` — Section List 형태의 정적 런처 페이지를 추가한다.

### 변경 파일
- `frontend/src/pages/admin-home-page.tsx`

### 핵심 로직
- `Helmet` title과 `noindex, nofollow`를 설정한다.
- `Link` 기반 행 2개를 렌더한다.
  - 서비스 피드백 관리 → `/admin/feedbacks`
  - 질문 풀 관리 → `/admin/question-pool`

## Phase 3: route/a11y 연결

- **구현**: `frontend` — 라우팅과 a11y smoke 대상에 `/admin`을 추가한다.

### 변경 파일
- `frontend/src/app.tsx`
- `frontend/tests/a11y/pages.spec.tsx`

### 핵심 로직
- `PasswordProtectedRoute` 아래에 `<Route path="/admin" element={<AdminHomePage />} />`를 추가한다.
- 기존 `/admin/feedbacks`, `/admin/question-pool` route는 유지한다.
- a11y smoke에 `AdminHomePage` 케이스를 추가한다.

## Phase 4: 검증

- [ ] `cd frontend && npm run test -- src/pages/__tests__/admin-home-page.test.tsx`
- [ ] `cd frontend && npm run test -- tests/a11y/pages.spec.tsx`
- [ ] `cd frontend && npm run build`

## 리뷰 게이트

- [ ] 구현 완료 후 변경 파일 자체 리뷰
- [ ] 테스트/a11y/build 결과 확인
