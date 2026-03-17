# Task 4: BE 내부 API 5개 + Internal-Api-Key 인증 필터

## Status: Not Started

## Issue: #82

## Why

Lambda는 DB에 직접 접근하지 않고, API 서버의 내부 엔드포인트를 호출하여 상태 변경 및 결과 저장을 수행.
외부 접근을 차단하기 위해 Internal-Api-Key 인증 필터 필요.

## 의존성

- 선행: Task 1 (DB 스키마)
- 후행: Task 6, 7 (Lambda에서 내부 API 호출)

## 구현 계획

### PR 1: [BE] Lambda용 내부 API 5개 + InternalApiKeyFilter

| 엔드포인트 | 메서드 | 용도 |
|------------|--------|------|
| `/api/internal/interviews/{id}/question-sets/{qsId}/answers` | GET | 답변 구간 메타데이터 조회 |
| `/api/internal/interviews/{id}/question-sets/{qsId}/progress` | PUT | 분석 진행 상태 업데이트 |
| `/api/internal/interviews/{id}/question-sets/{qsId}/feedback` | POST | 분석 결과 저장 |
| `/api/internal/interviews/{id}/question-sets/{qsId}/convert-status` | PUT | 변환 상태 + URL 업데이트 |
| `/api/internal/interviews/{id}/question-sets/{qsId}/retry-analysis` | POST | 수동 분석 재트리거 |

**InternalApiKeyFilter:**
- `OncePerRequestFilter` 상속
- `/api/internal/**` 경로만 적용
- `X-Internal-Api-Key` 헤더 검증
- 키 불일치 시 401 응답
- `application.yml`에 키 설정 (환경변수 바인딩)

**신규 파일:**
- `InternalQuestionSetController.java`
- `InternalApiKeyFilter.java`
- `SecurityConfig.java` 수정 (내부 API 경로 인증 제외 또는 별도 처리)

- Implement: `backend`
- Review: `architect-reviewer` — 보안, API 설계

## Acceptance Criteria

- [ ] 내부 API 5개 정상 동작
- [ ] `X-Internal-Api-Key` 없이 호출 시 401 반환
- [ ] 올바른 키로 호출 시 정상 응답
- [ ] progress 업데이트 시 `updated_at` 자동 갱신
- [ ] feedback 저장 시 analysis_status → COMPLETED 전환
- [ ] convert-status 업데이트 시 streaming_url 저장
