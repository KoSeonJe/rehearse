# Task 5: 좀비 감지 스케줄러 + 면접 완료 집계

## Status: Not Started

## Issue: #83

## Why

Lambda가 progress를 ANALYZING으로 바꾼 뒤 크래시하면, 질문세트가 영원히 ANALYZING 상태에 머무름.
1분마다 좀비 감지하여 10분 초과 시 FAILED로 전환해야 함.
또한 모든 질문세트가 COMPLETED되면 interview 전체를 완료 처리하고 overall 점수를 집계해야 함.

## 의존성

- 선행: Task 1 (DB 스키마)
- 후행: 없음

## 구현 계획

### PR 1: [BE] @Scheduled 좀비 감지 + 면접 완료 집계

**좀비 감지:**
```java
@Scheduled(fixedRate = 60000)
public void detectZombieAnalysis() {
    // ANALYZING + updated_at < 10분 전 → FAILED, failure_reason = "ANALYSIS_TIMEOUT"
}
```

**면접 완료 집계:**
- 모든 질문세트가 COMPLETED 상태일 때:
  - interview.status = COMPLETED
  - overall_score = 질문세트 점수 가중 평균
  - overall_comment = LLM 종합 코멘트 생성 (선택)

**신규 파일:**
- `AnalysisScheduler.java`
- `InterviewCompletionService.java` (또는 기존 InterviewService에 메서드 추가)

- Implement: `backend`
- Review: `code-reviewer` — 동시성, 트랜잭션

## Acceptance Criteria

- [ ] 1분마다 좀비 감지 쿼리 실행
- [ ] ANALYZING + 10분 초과 → FAILED + "ANALYSIS_TIMEOUT"
- [ ] 모든 질문세트 COMPLETED 시 interview.status = COMPLETED
- [ ] overall_score 정상 계산
- [ ] 스케줄러 로그 출력
