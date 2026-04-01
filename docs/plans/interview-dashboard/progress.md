# 면접 대시보드 — 진행 상황

## 태스크 상태

| # | 태스크 | 플랜 | 상태 | 태그 | 비고 |
|---|--------|------|------|------|------|
| 1 | 면접 목록/통계/삭제 API | `plan-01-backend-api.md` | Draft | [blocking] | BE API 완성 후 FE 진행 |
| 2 | 대시보드 페이지 UI | `plan-02-dashboard-page.md` | Draft | | Task 1 완료 후 진행 |
| 3 | 라우팅 분기 + 피드백 헤더 | `plan-03-routing.md` | Draft | [parallel] | Task 2와 병렬 가능 |

## 의존성

```
Task 1 (BE API) ──→ Task 2 (대시보드 UI)
                 ──→ Task 3 (라우팅 + 피드백 헤더) [parallel with Task 2]
```

## 진행 로그

### 2026-03-31
- 요구사항 정의 및 플랜 작성 완료
- 생성 파일: `requirements.md`, `plan-01-backend-api.md`, `plan-02-dashboard-page.md`, `plan-03-routing.md`, `progress.md`
