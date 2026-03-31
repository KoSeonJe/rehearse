# 피드백 리디자인 — 진행 상황

## 태스크 상태

| # | 태스크 | 상태 | 태그 | 비고 |
|---|--------|------|------|------|
| 1 | Gemini 프롬프트 재설계 | Draft | [parallel] | Plan 2, 3과 병렬 가능 |
| 2 | Vision 프롬프트 재설계 | Draft | [parallel] | Plan 1, 3과 병렬 가능 |
| 3 | Verbal 폴백 프롬프트 재설계 | Draft | [parallel] | Plan 1, 2와 병렬 가능 |
| 4 | Lambda handler.py 매핑 수정 | Draft | | Plan 1, 2, 3 완료 후 |
| 5 | BE 엔티티 + DTO 변경 | Draft | | 독립 개발 가능, 배포는 선행 |
| 6 | FE 피드백 패널 리디자인 | Draft | | Plan 5 완료 후 |

## 의존성 그래프

```
Plan 1 (Gemini) ──┐
Plan 2 (Vision) ──┼──→ Plan 4 (handler) ──→ Plan 5 (BE) ──→ Plan 6 (FE)
Plan 3 (Verbal) ──┘
```

## 배포 순서

```
PR-1 [BE] ──→ Lambda deploy.sh ──→ PR-2 [FE]
```

## 진행 로그

### 2026-03-30
- 요구사항 정의 및 전체 플랜 작성
- 생성 파일: requirements.md, plan-01 ~ plan-06, progress.md
