# Plan 01: 리포트 이벤트 통일 + 폴링 FE

> 상태: Draft
> 작성일: 2026-03-20
> 우선순위: P0
> 태그: [parallel] (Phase A)

## Why

현재 리포트 생성이 두 경로로 중복 트리거된다:
1. Lambda `handler.py` → `trigger_report` / `check_all_completed` → BE 내부 API 호출
2. FE 리포트 페이지 → `POST /api/interviews/{id}/report`

이 이중 경로가 race condition을 유발하며, Lambda 호출 실패 시 리포트가 아예 생성되지 않는 문제도 있다. 리포트 생성을 FE 요청 단일 경로로 통일하여 안정성을 확보한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/handler.py` | `trigger_report`, `check_all_completed` 호출 제거 |
| `lambda/analysis/api_client.py` | 리포트 관련 API 호출 함수 제거 |
| `backend/.../controller/InternalReportController.java` | 컨트롤러 제거 (내부 리포트 트리거 엔드포인트) |
| `backend/.../service/ReportService.java` | 멱등성 강화 (unique constraint, 중복 생성 방지) |
| `frontend/.../hooks/use-report.ts` | 202→"생성 중", 409→"분석 중" 상태 분기 처리 |
| `frontend/.../pages/interview-report-page.tsx` | 전용 대기 UI (생성 중 / 분석 중 구분) |

## 상세

### BE 변경

1. **InternalReportController 제거**
   - Lambda에서 직접 호출하던 내부 리포트 생성 엔드포인트 삭제
   - 리포트 생성은 `POST /api/interviews/{id}/report` 단일 경로만 유지

2. **ReportService 멱등성 강화**
   - `Interview` + `Report` unique constraint 추가 (중복 리포트 방지)
   - 이미 리포트 존재 시 409 Conflict 반환
   - 분석 미완료 시 202 Accepted 반환 + "분석 중" 메시지

### Lambda 변경

3. **handler.py에서 리포트 트리거 제거**
   - 분석 완료 후 `trigger_report` / `check_all_completed` 호출 코드 삭제
   - 분석 결과 저장만 수행하고 종료

### FE 변경

4. **useReport 훅 응답 분기**
   - `200`: 리포트 데이터 표시
   - `202`: "리포트 생성 중..." 대기 UI + 폴링
   - `409`: "분석이 아직 진행 중입니다" 안내 UI
   - `500`: 에러 표시 (Task 4에서 상세화)

5. **대기 UI**
   - 생성 중: 프로그레스 인디케이터 + 예상 시간 안내
   - 분석 중: 분석 완료 후 리포트 생성 가능 안내 + 폴링

## 담당 에이전트

- Implement (BE): `backend` — 리포트 서비스 멱등성 + 컨트롤러 제거
- Implement (Lambda): `backend` — handler.py 리포트 트리거 제거
- Implement (FE): `frontend` — useReport 훅 + 대기 UI
- Review: `architect-reviewer` — 이벤트 경로 단일화 검증, 레이어링

## 검증

- Lambda 분석 완료 후 리포트 자동 생성 호출이 없는지 확인
- FE에서 리포트 요청 시 202/409/200 응답 각각 올바른 UI 표시
- 중복 리포트 생성 시도 시 409 반환 확인
- `progress.md` 상태 업데이트 (Task 1 → Completed)
