# Plan 03: 라우팅 분기 + 피드백 페이지 헤더

> 상태: Completed
> 작성일: 2026-03-31

## Why

로그인 사용자는 랜딩페이지가 아닌 대시보드를 메인으로 사용해야 한다. 또한 피드백 페이지에 면접 정보 헤더를 추가하여 대시보드에서의 진입 경험을 개선한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/app.tsx` | `/dashboard` 라우트 추가, `/` 분기 로직 |
| `frontend/src/pages/home-page.tsx` | 로그인 상태 시 `/dashboard` 리다이렉트 |
| `frontend/src/pages/interview-feedback-page.tsx` | 상단 면접 정보 헤더 추가, "닫기" → "대시보드로" |

## 상세

### 라우팅 변경 (app.tsx)

```tsx
<Routes>
  <Route path="/" element={<HomePage />} />
  <Route element={<ProtectedRoute />}>
    <Route path="/dashboard" element={<DashboardPage />} />
    <Route path="/interview/setup" element={<InterviewSetupPage />} />
    {/* ... 기존 라우트 */}
  </Route>
  <Route path="*" element={<Navigate to="/" replace />} />
</Routes>
```

### HomePage 분기

```tsx
// home-page.tsx
const { isAuthenticated, isLoading } = useAuth()
const navigate = useNavigate()

useEffect(() => {
  if (!isLoading && isAuthenticated) {
    navigate('/dashboard', { replace: true })
  }
}, [isLoading, isAuthenticated, navigate])
```

### 피드백 페이지 면접 정보 헤더

기존 Hero 섹션 위에 면접 정보 요약 바 추가:

```
┌─────────────────────────────────────────────┐
│ [← 대시보드] Logo 타임스탬프 리뷰            │
├─────────────────────────────────────────────┤
│ BACKEND · Spring Boot 백엔드                │
│ CS, PROJECT · 30분 · 2026-03-30            │
└─────────────────────────────────────────────┘
```

- 데이터는 기존 `useInterviewByPublicId` 응답에서 추출 (추가 API 불필요)
- "닫기" 버튼 → `navigate('/dashboard')`로 변경

## 담당 에이전트

- Implement: `frontend` — 라우팅 수정, 컴포넌트 수정
- Review: `code-reviewer` — 리다이렉트 로직 검증

## 검증

- 비로그인 `/` → 랜딩페이지 표시
- 로그인 `/` → `/dashboard` 리다이렉트
- 비로그인 `/dashboard` → 로그인 모달
- 피드백 페이지 면접 정보 헤더 표시
- "대시보드로" 클릭 시 `/dashboard` 이동
- `progress.md` 상태 업데이트 (Task 3 → Completed)
