# 피드백 페이지 v2 — 진행 상황

## 태스크 상태

| # | 태스크 | 상태 | 태그 | 비고 |
|---|--------|------|------|------|
| 1 | Gemini 프롬프트 (structure + attitude) | Draft | [parallel] | Plan 2와 병렬 가능 |
| 2 | Vision 프롬프트 (노트북 시선 보정) | Draft | [parallel] | Plan 1과 병렬 가능 |
| 3 | Lambda handler 필드 매핑 | Draft | | Plan 1, 2 완료 후 |
| 4 | BE 엔티티 + DTO + DB 마이그레이션 | Draft | [blocking] | 독립 개발 가능, 배포는 선행 |
| 5 | FE 2탭 재편 + 가독성 개선 | Draft | | Plan 4 완료 후 |

## 의존성 그래프

### 개발 순서
```
Plan 1 (Gemini) ──┐
                   ├──→ Plan 3 (handler) ──→ Plan 4 (BE) ──→ Plan 5 (FE)
Plan 2 (Vision) ──┘
```

### 배포 순서 (개발 순서와 다름!)
```
PR-1 [BE] (Plan 4) ──→ Lambda deploy.sh (Plan 1,2,3) ──→ PR-2 [FE] (Plan 5)
```
> BE 먼저 배포: nullable 컬럼이라 Lambda 미배포 상태에서도 안전.

## 진행 로그

### 2026-04-01
- 요구사항 정의 및 전체 플랜 작성
- 생성 파일: requirements.md, plan-01 ~ plan-05, progress.md
