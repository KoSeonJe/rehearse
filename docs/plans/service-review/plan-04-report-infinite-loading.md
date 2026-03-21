# Plan 04: 리포트 무한로딩 수정

> 상태: Draft
> 작성일: 2026-03-20
> 우선순위: P0
> 태그: [blocking] (Phase B — Task 1 완료 후)

## Why

리포트 페이지에서 무한로딩이 발생한다. 주요 원인은 Task 1(이중 리포트 경로)이나, Task 1 완료 후에도 잔여 원인이 있을 수 있다:
- `generateReport` 내부에서 Claude API 호출 실패 시 에러가 적절히 전파되지 않음
- FE 폴링이 500 에러를 받아도 계속 재시도하여 무한 대기

Task 1 완료 후 잔여 문제를 확인하고, BE 에러 코드 + FE 에러 핸들링을 보강한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/.../service/ReportService.java` | generateReport 실패 시 명확한 에러 코드 반환 |
| `backend/.../exception/ReportErrorCode.java` | REPORT_GENERATION_FAILED 에러 코드 추가 |
| `frontend/.../hooks/use-report.ts` | 500 응답 시 즉시 에러 표시, 폴링 타임아웃 추가 |
| `frontend/.../pages/interview-report-page.tsx` | 에러 상태 UI (생성 실패, 타임아웃) |

## 상세

### BE 변경

1. **ReportErrorCode 추가**
   - `REPORT_GENERATION_FAILED`: Claude API 호출 실패, 파싱 에러 등
   - `REPORT_ALREADY_EXISTS`: 중복 생성 시도 (409)
   - `ANALYSIS_NOT_COMPLETED`: 분석 미완료 상태 (202)

2. **ReportService 에러 처리 강화**
   - `generateReport` 내부 Claude API 호출을 try-catch로 감싸 실패 시 명확한 에러 코드 반환
   - 리포트 생성 상태 추적 (생성 시작 → 완료/실패)

### FE 변경

3. **useReport 폴링 개선**
   - 500 응답 시 즉시 폴링 중단 + 에러 UI 표시
   - 폴링 타임아웃: 최대 2분 (리포트 생성 예상 시간 기준)
   - 타임아웃 시 "리포트 생성에 시간이 걸리고 있습니다. 잠시 후 다시 확인해주세요." 안내

4. **에러 UI**
   - 생성 실패: "리포트 생성에 실패했습니다" + 재시도 버튼
   - 폴링 타임아웃: "생성이 지연되고 있습니다" + 새로고침 안내

## 담당 에이전트

- Implement (원인 분석): `debugger` — Task 1 완료 후 잔여 무한로딩 원인 분석
- Implement (BE): `backend` — 에러 코드 + 리포트 서비스 에러 처리
- Implement (FE): `frontend` — 폴링 개선 + 에러 UI
- Review: `architect-reviewer` — 에러 처리 흐름 일관성

## 검증

- 리포트 생성 API 500 에러 시 FE가 즉시 에러 표시하는지 확인
- 폴링 타임아웃(2분) 후 명확한 안내 메시지 표시
- 정상 케이스(리포트 생성 성공)에서 기존 동작과 동일한지 확인
- Task 1 완료 후 무한로딩 재현 불가능한지 확인
- `progress.md` 상태 업데이트 (Task 4 → Completed)
