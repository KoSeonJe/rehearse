# Implement — 진행차단진단 식별자 enum 도입 + magic number 상수화

> **작성자**: backend agent
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: BE only 단일 PR.

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 |
|-------|------|--------------|--------|------|
| 1 | enum / 상수 정의 + 단위 테스트 | `backend` | 단일 PR | - |
| 2 | 호출부 7개소 + magic number 적용 | `backend` | 단일 PR | Phase 1 |
| 3 | 통합 회귀 + 빌드 검증 | `backend` | 단일 PR | Phase 2 |

---

## Phase 1: enum / 상수 정의 + 단위 테스트

- **구현**: `backend` — 도메인 entity 패키지 enum / 상수 추가, Domain Unit 테스트 동시 작성

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/interview/entity/BlockReason.java` (신규) — 진행차단진단 reason 식별자 6종 enum + 명시 매핑 (`logValue()`)
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/InterviewTrack.java` (변경) — `logLabel()` 메서드 추가 (`RESUME → "RESUME"`, `CS / LANGUAGE → "STANDARD"`)
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ChainStateTracker.java` (변경) — `public static final int MAX_LEVEL = 4` 추가 (기존 `LEVEL_STAY_MAX_TURNS` 패턴 동일)
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java` (변경) — `private static final int DEFAULT_ANSWER_QUALITY = 2` 추가
- `backend/src/test/java/com/rehearse/api/domain/interview/entity/BlockReasonTest.java` (신규) — 6종 매핑 assertion
- `backend/src/test/java/com/rehearse/api/domain/interview/entity/InterviewTrackTest.java` (신규 또는 기존) — `logLabel()` 케이스 3종

### 핵심 로직

- `BlockReason`: enum 값 6종 + `private final String value` 필드 + 생성자 + `logValue()` getter. **자동 변환 사용 X** (운영 grep 출력 호환 위해 명시 매핑).
- `InterviewTrack.logLabel()`: `this == RESUME ? "RESUME" : "STANDARD"`. 멤버 변경 X.
- `ChainStateTracker.MAX_LEVEL`: public 노출 (외부 사용처 `InterrogationModeHandler:115` 1곳).
- `InterrogationModeHandler.DEFAULT_ANSWER_QUALITY`: private 상수.

### 의존

- 선행: 없음
- 외부: 없음

### Verification Hook

- 명령: `./gradlew test --tests "com.rehearse.api.domain.interview.entity.BlockReasonTest" --tests "com.rehearse.api.domain.interview.entity.InterviewTrackTest"`
- 통과 기준: green
- Domain Unit (`DomainUnitSupport` 또는 순수 단위) — Spring 컨텍스트 불필요

---

## Phase 2: 호출부 7개소 + magic number 적용

- **구현**: `backend` — 호출부 string literal → enum 호출 치환, raw int → 상수 치환

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java`
  - `:102` `publish-skip` → `BlockReason.PUBLISH_SKIP.logValue()`, `track=RESUME` → `track={}` + `InterviewTrack.RESUME.logLabel()`
  - `:217` `questionId-missing` 동일 패턴 + `type={}` placeholder 유지
  - `:226` `response-questionid-missing` 동일 + `handlerQuestionId={}` 유지
  - `:231` `response-questionid-mismatch` 동일 + `handlerQuestionId={} responseQuestionId={}` 유지
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeTurnEventPublisher.java`
  - `:32` `track=RESUME` + `questionId-missing` 동일 패턴
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java`
  - `:86` `track=STANDARD reason=analyzer-skip` → `InterviewTrack.CS.logLabel()` + `BlockReason.ANALYZER_SKIP.logValue()` (stage `"standard-followup"` literal 유지)
  - `:111` `step-b-skip` 동일 패턴
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java`
  - `:81` raw `2` → `DEFAULT_ANSWER_QUALITY`
  - `:115` raw `4` → `ChainStateTracker.MAX_LEVEL`
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ChainStateTracker.java`
  - `:66` raw `4` → `MAX_LEVEL`
  - `:91` raw `4` → `MAX_LEVEL`

### 핵심 로직 (호출부 패턴)

```java
log.warn("[진행차단진단] interviewId={} track={} stage={} reason={} turnIndex={}",
        interviewId,
        InterviewTrack.RESUME.logLabel(),
        currentMode.name().toLowerCase(),
        BlockReason.PUBLISH_SKIP.logValue(),
        turnIndex);
```

추가 키 보유 호출부 (`:217 :226 :231`) = 동일 패턴 + 추가 placeholder + 인자 추가 (기존 키 / 위치 보존).

### 의존

- 선행: Phase 1 (enum / 상수 정의)
- 외부: 없음

### Verification Hook

- 명령: `./gradlew compileJava` (컴파일 통과 = 식별자 정합성)
- grep 검증:
  - `grep -rn '"publish-skip"\|"questionId-missing"\|"response-questionid-missing"\|"response-questionid-mismatch"\|"analyzer-skip"\|"step-b-skip"' backend/src/main/java/com/rehearse` → `BlockReason.java` 외 0건
  - `grep -rn 'currentLevel.*[<>=].*4\b' backend/src/main/java` → `MAX_LEVEL` 외 0건
  - `grep -rn ': 2;' backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java` → 0건

---

## Phase 3: 통합 회귀 + 빌드 검증

- **구현**: `backend` — Service Integration 1건 추가 + 기존 통합 테스트 회귀

### 변경 파일

- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestratorTest.java` (또는 동등 통합 테스트) — `[진행차단진단]` 시나리오 1종 (publish-skip 추천) 트리거 + 로그 캡처 + 키 5개 등장 assertion (`OutputCaptureExtension` 사용)

### 핵심 로직 (테스트 스케치)

```java
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("진행차단진단 publish-skip 시나리오에서 키 스키마 보존된다")
@Test
void warnLog_preservesKeySchema_when_publishSkip(CapturedOutput output) {
    // given: publish-skip 트리거 조건 셋업 (questionId 부재 인터뷰 등)
    ...
    // when: orchestrator 호출
    ...
    // then
    assertThat(output.getOut())
        .contains("[진행차단진단]")
        .contains("track=RESUME")
        .contains("stage=playground")
        .contains("reason=publish-skip")
        .contains("interviewId=", "turnIndex=");
}
```

### 의존

- 선행: Phase 2
- 외부: 없음 (Spring `OutputCaptureExtension` 또는 LogCaptor)

### Verification Hook

- 명령: `./gradlew clean build`
- 통과 기준: 전체 green + warning 신규 0
- 회귀: 기존 `ResumeInterviewOrchestrator` / `FollowUpService` / `ChainStateTracker` 통합 테스트 통과

### 커밋 메시지 (단일)

```
refactor(BE): 진행차단진단 식별자 enum 도입 + magic number 상수화
```

---

## 통합 Verification

tech-spec.md Verification 섹션 참조.

- [ ] Domain Unit (`BlockReasonTest`, `InterviewTrackTest`, `ChainStateTrackerTest` 회귀) green
- [ ] Service Integration 1건 (`[진행차단진단]` 로그 캡처) green
- [ ] `./gradlew clean build` 통과
- [ ] grep 잔존 0건 (tech-spec Verification 명시)
- [ ] 운영 grep 패턴 호환 (key 스키마 동등)

## 리뷰 게이트 (MANDATORY)

- [ ] BE only → `code-reviewer-backend` 호출
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md`, `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md Pre/Post 섹션)
