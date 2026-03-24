# Virtual Thread Migration — 진행 상황

## 태스크 상태

| # | 태스크 | Plan | 상태 | 태그 |
|---|--------|------|------|------|
| 1 | VT 활성화 + HikariCP 업그레이드 | plan-01 | Completed | [blocking] |
| 2 | generateFollowUp() TX 분리 | plan-02 | Completed | [blocking] |
| 3 | AsyncConfig 정리 | plan-03 | Completed | |
| 4 | Resilience4j RateLimiter | plan-04 | Completed | [parallel] |
| 5 | WhisperService timeout | plan-05 | Completed | [parallel] |
| 6 | 단위/통합 테스트 | plan-06 | Completed | |
| 7 | 부하테스트 (TX 분리 + VT 전후) | plan-07 | Completed | |
| 8 | 부하테스트 결과 문서화 | plan-08 | Completed | |

## 진행 로그

### 2026-03-24 (부하테스트)
- Task 7~8 완료
- 수정 파일:
  - `backend/src/test/k6/mock-server.py` (ThreadingMixIn 멀티스레드 전환)
  - `backend/src/test/k6/follow-up-load-test.js` (handleSummary 버그 수정, VU 기반 interview 분배)
- 생성 파일:
  - `docs/plans/virtual-thread-migration/load-test-results.md` (부하테스트 결과 보고서)
- 테스트 결과 요약:
  - 시나리오 A: TX 분리 후 커넥션 점유 98% 감소, 50 VUs 에러율 0%
  - 시나리오 B: VT 도입으로 JVM 스레드 79% 감소 (203→44), 처리량 동일
  - 시나리오 C: RateLimiter 18.7 req/s 처리 (한도 20 근접), 대기 큐 방식 동작 확인

### 2026-03-24
- Task 1~6 전체 구현 완료
- 생성 파일:
  - `docs/plans/virtual-thread-migration/` (requirements.md, plan-01~08, progress.md)
  - `FollowUpTransactionHandler.java` (TX 분리 핸들러)
  - `FollowUpContext.java` (Phase 간 데이터 전달 record)
  - `FollowUpTransactionHandlerTest.java` (단위 테스트 5개)
- 수정 파일:
  - `build.gradle.kts` (HikariCP 6.2.1, Resilience4j, AOP, bootRun JVM 옵션)
  - `application-prod.yml` (VT 활성화, HikariCP 풀 설정, RateLimiter 설정)
  - `InterviewService.java` (generateFollowUp TX 분리 + resolveAnswerText 추출)
  - `AsyncConfig.java` (executor Bean 2개 제거)
  - `QuestionGenerationEventHandler.java` (@Async qualifier 제거)
  - `QuestionGenerationService.java` (VT executor 전환)
  - `ClaudeApiClient.java` (@RateLimiter 적용)
  - `WhisperService.java` (timeout + @RateLimiter 적용)
  - `GlobalExceptionHandler.java` (RequestNotPermitted 핸들러 추가)
  - `InterviewServiceTest.java` (FollowUpTransactionHandler mock 사용으로 업데이트)
- 전체 테스트 155개 통과
