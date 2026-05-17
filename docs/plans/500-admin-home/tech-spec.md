# Tech Spec — Admin Home

> **작성자**: frontend agent
> **답하는 질문**: `/admin` 어드민 런처를 어떻게 추가할 것인가
> **승인 게이트**: 사용자 명시 승인 완료 후 구현

---

## Why → Goal

관리자가 어드민 하위 URL을 직접 기억하지 않고 `/admin`에서 주요 관리 화면으로 이동할 수 있게 한다.

## Evidence

- GitHub Issue #500: `/admin` 런처 페이지, 서비스 피드백 관리와 질문 풀 관리 링크, 기존 직접 접근 유지, 테스트/a11y/build 검증.
- 현재 라우팅: `frontend/src/app.tsx`에서 `/admin/feedbacks`, `/admin/question-pool`이 `PasswordProtectedRoute` 아래에 있다.
- 현재 인증: `frontend/src/components/ui/password-protected-route.tsx`가 `sessionStorage['admin-password']` 기반으로 관리자 인증 상태를 유지한다.
- 사용자 결정: visual companion에서 `Section List` 형태로 진행하기로 확정했다.

## Trade-offs

### Option A (채택) — Section List 런처
- 장점: 두 관리 화면을 행 단위로 빠르게 스캔할 수 있고 모바일/데스크톱 모두 단순하다.
- 단점: 카드형보다 시각적 여백은 적다.
- 사유: 사용자가 선택한 방향이고, 이슈 범위가 링크 제공에 한정되어 있다.

### Option B (폐기) — Compact Cards 런처
- 장점: 데스크톱에서 시각적으로 넓고 균형 잡힌다.
- 단점: 사용자가 선택한 방향이 아니다.
- 폐기 사유: 명시 선택을 우선한다.

### Option C (폐기) — Status Dashboard
- 장점: 운영 대시보드처럼 풍부해 보인다.
- 단점: 새 API 또는 가짜 지표가 필요하다.
- 폐기 사유: Issue #500 범위를 넘어선다.

## Architecture

```
[Browser] → /admin
  → [PasswordProtectedRoute]
    → [AdminHomePage]
      ├─ Link /admin/feedbacks
      └─ Link /admin/question-pool
```

## Data Model

DB, API, 서버 상태 변경 없음.

## API Contract

신규 API 없음. 기존 관리자 인증과 기존 관리 페이지 API를 그대로 사용한다.

## Verification

- [ ] `cd frontend && npm run test -- src/pages/__tests__/admin-home-page.test.tsx`
- [ ] `cd frontend && npm run test -- tests/a11y/pages.spec.tsx`
- [ ] `cd frontend && npm run build`

## Pre / Post State

### Pre
- `/admin` route가 없다.
- 관리자는 `/admin/feedbacks`, `/admin/question-pool`을 직접 알아야 한다.

### Post
- `/admin` route가 `PasswordProtectedRoute` 아래에 추가된다.
- `/admin`에서 서비스 피드백 관리와 질문 풀 관리 링크를 볼 수 있다.
- 기존 직접 접근 route는 유지된다.

## 위험 / 마이그레이션 / 롤백

- 위험: route import 추가로 build/type 오류가 날 수 있다.
- 마이그레이션: 없음.
- 롤백: `AdminHomePage` 파일과 `/admin` route, 관련 테스트/a11y 항목 제거.

## 분기 결정

- [x] 단일 영역 → `implement-fe.md` 1개
- [ ] BE+FE 동시
- [ ] BE 선행 강제
