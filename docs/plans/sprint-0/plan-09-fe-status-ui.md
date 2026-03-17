# Task 9: FE 분석 대기 + 상태 추적 UI

## Status: Not Started

## Issue: #87

## Why

면접 종료 후 마지막 질문세트 분석을 기다리는 동안 사용자에게 진행 상태를 보여줘야 함.
질문세트별 분석 상태를 개별 표시하고, 이미 완료된 결과는 미리보기 제공.

상세 흐름: `docs/architecture/recording-analysis-pipeline.md` 4단계

## 의존성

- 선행: Task 6 (분석 Lambda), Task 7 (변환 Lambda), Task 8 (FE 녹화)
- 후행: Task 10 (분석 완료 후 피드백 뷰어 전환)

## 구현 계획

### PR 1: [FE] 분석 대기 페이지 + 상태 폴링

**상태 폴링:**
- GET /api/interviews/{id}/status — 5초 간격 폴링 (TanStack Query refetchInterval)
- 질문세트별 analysisStatus + analysisProgress + convertStatus

**UI 구성:**
- 상단: "AI가 면접 영상을 분석하고 있습니다. 약 2~5분 정도 소요됩니다."
- 질문세트 카드 리스트:
  - STARTED → "분석 준비 중..."
  - EXTRACTING → "음성/영상 추출 중..."
  - STT_PROCESSING → "음성을 텍스트로 변환 중..."
  - VERBAL_ANALYZING → "답변 내용을 분석 중..."
  - NONVERBAL_ANALYZING → "표정과 자세를 분석 중..."
  - FINALIZING → "종합 평가를 생성 중..."
  - COMPLETED → 체크마크 + "분석 완료" + 미리보기 링크
  - FAILED → "분석 실패" + 재시도 버튼

**완료 처리:**
- 모든 질문세트 COMPLETED → 피드백 페이지 자동 전환
- 일부 실패: "나머지 결과를 먼저 확인하시겠습니까?" 옵션

**모범답변 탭:**
- 분석 대기 중 GET /questions-with-answers로 모범답변 즉시 조회

**알림:**
- 페이지 이탈 시 Web Notification API로 완료 알림

- Implement: `frontend`
- Review: `designer` + `code-reviewer`

## Acceptance Criteria

- [ ] 5초 간격 상태 폴링
- [ ] 질문세트별 분석 진행 상태 실시간 표시
- [ ] 단계별 메시지 + 프로그레스 바
- [ ] COMPLETED 시 미리보기 링크
- [ ] FAILED 시 재시도 버튼
- [ ] 모든 완료 시 자동 전환
- [ ] 모범답변 탭 즉시 조회 가능
