# 서비스 리뷰 — 진행 상황

## 태스크 상태

| # | 태스크 | 우선순위 | 상태 | Phase | 비고 |
|---|--------|----------|------|-------|------|
| 1 | 리포트 이벤트 통일 + 폴링 FE | P0 | Draft | A (병렬) | BE + Lambda + FE |
| 2 | 스킵 질문 완전 숨김 | P1 | Draft | A (병렬) | FE only |
| 3 | 분석 실패/타임아웃 UI | P1 | Draft | A (병렬) | BE + Lambda + FE |
| 4 | 리포트 무한로딩 수정 | P0 | Draft | B (Task 1 후) | Task 1 의존 |
| 5 | 노션 + 로컬 문서 보완 | P2 | Draft | A (병렬) | Docs only |

## 의존성

```
Phase A: Task 1, 2, 3, 5 (병렬 진행 가능)
Phase B: Task 4 (Task 1 완료 필요)
```

## 예상 PR 수: ~8개

| PR | 범위 | 태스크 |
|----|------|--------|
| 1 | [BE] 리포트 이벤트 통일 | Task 1 |
| 2 | [Lambda] 리포트 트리거 제거 | Task 1 |
| 3 | [FE] 리포트 대기 UI | Task 1 |
| 4 | [FE] 스킵 질문 숨김 | Task 2 |
| 5 | [BE] 실패 코드 표준화 | Task 3 |
| 6 | [FE] 실패/타임아웃 UI | Task 3 |
| 7 | [BE/FE] 리포트 무한로딩 수정 | Task 4 |
| 8 | [Docs] 문서 보완 | Task 5 |

## 진행 로그

### 2026-03-20
- 스펙 문서 7개 생성 (requirements.md, plan-01~05, progress.md)
