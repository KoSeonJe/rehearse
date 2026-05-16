# Task 13 — Service Integration + E2E + 결정적 회귀 + 로그 마스킹 테스트

> **위치**: `tasks/p2-be-13-tests.md`
> **답하는 질문**: P2 전체 회귀 어떻게 보장?

---

## 목적

tech-spec §Verification 항목 중 Phase 2 책임 테스트 일괄 추가. Service Integration / E2E / 결정적 회귀 / 로그 마스킹 / JSON 호환.

## 에이전트

- **구현**: `backend` — Testcontainers MySQL + LLM mock fixture + logback capture + ArchUnit 보조
- **리뷰**: `code-reviewer-backend` — 테스트 커버리지 / mock vs Live LLM 분리 / 결정성

## 변경 파일

**Service Integration 테스트**:
- `backend/src/test/.../ResumeTrackInitiatorIntegrationTest.java` — Task 05 정합 (opener N + main M 일괄 생성, RubricCategory 매핑, projects 범위 topic)
- `backend/src/test/.../FollowUpServiceResumeFlowTest.java` — Task 06 정합 (RESUME_OPENER → follow-up 미생성 / RESUME_MAIN → RESUME_FOLLOWUP 1개)
- `backend/src/test/.../AnswerAnalyzerPromptRenderingTest.java` — Task 03 정합 (`dimension_gaps` / `weakest_dimension` 토큰 포함, `missing_perspectives` / `askedPerspectives` 부재)

**Domain Unit 테스트**:
- `backend/src/test/.../AnswerAnalysisTest.java` — Task 02 정합 (5 필드 record 컴파일 + `applyL1FalseNegativeGuard` 부재)

**E2E 테스트**:
- `backend/src/test/.../ResumeInterviewE2ETest.java` — `/interviews` 생성 → `/interviews/{id}/status` IN_PROGRESS → opener 응답 → main 응답 (`/follow-up`) → RESUME_FOLLOWUP 응답 → 시간 만료 종료. 성공 1 케이스

**결정적 회귀**:
- `backend/src/test/.../ResumeRepetitionDeterministicTest.java` — interview 29 trace 기반 mock LLM 응답 시퀀스. 동일 topic 3회 이상 출제 발생 시 fail. Live LLM 미의존 (결정성 격리)

**Live 회귀 (`@Disabled` + env gate)**:
- `backend/src/test/.../ResumeRepetitionRegressionTest.java` — `@EnabledIfEnvironmentVariable(named="RUN_LIVE_API", matches="true")`. interview 29 답변 패턴 e2e 시나리오로 Live LLM 호출. 동일 topic 3회 이상 발생 시 fail

**JSON 호환**:
- `backend/src/test/.../ResumeSkeletonJsonCompatibilityTest.java` — 기존 `interrogationPriorityMap` 필드 포함 JSON payload 입력 시 `@JsonIgnoreProperties(ignoreUnknown = true)` 무시 + record 생성 성공 (P3 진행이지만 P2 머지 시점 호환 가드 확인)

**로그 마스킹**:
- `backend/src/test/.../ResumeTrackInitiatorLoggingTest.java` — Task 12 정합

## 핵심 로직

```java
// ResumeRepetitionDeterministicTest 예시
@Test
void interview29_pattern_replay_topic_repeat_under_3() {
    // Given: interview 29 와 동일한 답변 패턴 (동일 topic 반복 답변)
    List<String> answers = List.of(/* interview 29 trace */);
    Map<Integer, GeneratedFollowUp> mockLlmSequence = Map.of(/* 모킹 */);

    // When: e2e 시뮬레이션
    var result = simulateInterview(answers, mockLlmSequence);

    // Then: 동일 topic 3회 이상 출제 부재
    Map<String, Long> topicCount = result.questions().stream()
        .collect(groupingBy(Question::getTopic, counting()));
    assertThat(topicCount.values()).allMatch(c -> c < 3);
}
```

## 의존
- 선행 Task: 01-12 모두 완료 후 진행 (회귀 가드)
- 외부: Testcontainers MySQL + LLM mock fixture

## 테스트 케이스
- [ ] `./gradlew test` 전체 green
- [ ] mock LLM 결정성 = `ResumeRepetitionDeterministicTest` 100회 실행 100% 동일 결과
- [ ] Live LLM gate = `RUN_LIVE_API=false` 환경에서 skip 정상
- [ ] E2E 시간 만료 종료 = FE terminate 신호 mock

## 완료 기준
- [ ] tech-spec §Verification 항목 (P2 책임) 모두 green
- [ ] 결정적 + Live 두 회귀 테스트 동시 존재
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
test(BE): Resume 트랙 P2 회귀 - Service Integration + E2E + 결정적 회귀
```

## 비고

- R5 위험 (LLM 일관성 회복 측정 부재) 완화 = 결정적 + Live 두 축 테스트
- Live 회귀 비결정성 격리 = `@Disabled` + env gate 패턴
