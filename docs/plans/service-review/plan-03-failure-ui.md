# Plan 03: 분석 실패/타임아웃 UI

> 상태: Draft
> 작성일: 2026-03-20
> 우선순위: P1
> 태그: [parallel] (Phase A)

## Why

분석 Lambda가 실패하거나 타임아웃되었을 때 FE에서 아무 안내 없이 "분석 중" 상태가 지속된다.
사용자는 영원히 대기하게 되며, 실패 원인도 알 수 없다.
실패 유형별 안내와 부분 실패 시 "완료된 결과 먼저 보기" 옵션을 제공한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/.../domain/QuestionSet.java` | `failureReason` 필드 추가 (TIMEOUT, API_ERROR, INTERNAL_ERROR 등) |
| `lambda/analysis/handler.py` | 에러 분류 로직 추가 → failureReason 코드와 함께 FAILED 상태 전송 |
| `frontend/.../pages/interview-analysis-page.tsx` | 실패 유형별 안내 UI + 부분 실패 처리 |
| `frontend/.../pages/interview-feedback-page.tsx` | 실패 질문세트 안내 배너 |

## 상세

### BE 변경

1. **failureReason 표준 코드**
   - `QuestionSet` 엔티티에 `failureReason` 필드 추가 (nullable String)
   - 표준 코드: `TIMEOUT`, `API_ERROR`, `TRANSCRIPTION_ERROR`, `VISION_ERROR`, `INTERNAL_ERROR`
   - 분석 실패 시 Lambda가 내부 API로 failureReason과 함께 FAILED 상태 전송

### Lambda 변경

2. **에러 분류 로직**
   - `handler.py`에서 예외 타입별 failureReason 매핑
   - OpenAI API 에러 → `API_ERROR`
   - FFmpeg/Whisper 에러 → `TRANSCRIPTION_ERROR`
   - Vision 분석 에러 → `VISION_ERROR`
   - 타임아웃 → `TIMEOUT`
   - 기타 → `INTERNAL_ERROR`

### FE 변경

3. **분석 페이지 실패 UI**
   - 전체 실패: 실패 유형별 안내 메시지 + 재시도 버튼
     - TIMEOUT: "분석 시간이 초과되었습니다. 다시 시도해주세요."
     - API_ERROR: "외부 서비스 연결에 실패했습니다."
     - 기타: "분석 중 오류가 발생했습니다."
   - 부분 실패: "N개 질문 분석 완료 / M개 실패" + "완료된 결과 먼저 보기" 버튼

4. **피드백 페이지 실패 배너**
   - FAILED 질문세트가 있으면 상단에 경고 배너
   - "일부 질문의 분석에 실패했습니다" + 실패 사유 요약

## 담당 에이전트

- Implement (BE): `backend` — failureReason 필드 + API 수정
- Implement (Lambda): `backend` — 에러 분류 로직
- Implement (FE): `frontend` — 실패 UI + 부분 실패 처리
- Review: `architect-reviewer` — 에러 코드 설계, BE/Lambda/FE 일관성

## 검증

- 각 실패 유형(TIMEOUT, API_ERROR 등)에 대해 올바른 UI 메시지 표시
- 부분 실패 시 완료된 결과만 먼저 볼 수 있는지 확인
- failureReason이 null인 기존 데이터와의 호환성 확인
- `progress.md` 상태 업데이트 (Task 3 → Completed)
