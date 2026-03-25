# 분석 파이프라인 상태 관리 재설계 — 진행 상황

## 태스크 상태

| # | 태스크 | 상태 | 태그 | 비고 |
|---|--------|------|------|------|
| 1 | QuestionSetAnalysis 엔티티 + Enum + DB 마이그레이션 | Draft | [blocking] | BE 기반 작업 |
| 2 | Service + API 변경 (BE) | Draft | [blocking] → Task 1 | |
| 3 | Lambda analysis 수정 | Draft | [parallel: Task 4] | **BE 배포 후** Lambda 배포 |
| 4 | Lambda convert 수정 | Draft | [parallel: Task 3] | **BE 배포 후** Lambda 배포 |
| 5 | FE 타입 + 폴링 + UI 수정 | Draft | | Task 2 완료 후 |
| 6 | 테스트 보강 | Draft | | Task 2,3,4 완료 후 |

## 배포 순서 (필수)

```
1. BE 배포 (Task 1 + 2) — 새 엔드포인트 + 마이그레이션
2. Lambda 배포 (Task 3 + 4) — BE 새 API가 live된 후에만
3. FE 배포 (Task 5) — BE 응답 스키마 변경 반영
```

**주의**: Lambda가 BE보다 먼저 배포되면 새 상태 문자열/엔드포인트로 호출 시 400/404 발생

## 진행 로그

### 2026-03-24
- 요구사항 정의 및 계획 문서 작성
- BE/FE/Lambda 전체 영향 범위 분석 완료
- 영향 파일: BE 15+, Lambda 4, FE 6+
