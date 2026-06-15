# Tech Spec — 진행차단진단 식별자 enum 도입 + magic number 상수화

> **작성자**: backend agent (Claude Staff Engineer 페르소나 보조)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

`product-spec.md` 의 Why/Goal 1줄 요약: 진행차단진단 WARN 로그 7개소 string literal 산재 + magic number 잔존을 식별자 enum + 명명 상수로 통합해 새 reason / 트랙 / 정책 추가 시 회귀 위험 제거.

**Issue #430 매핑**: 본 tech-spec = 6항목 중 #2 (BlockReason enum) + #3 (InterviewTrack 매핑) + #6 (magic number 상수화). #1 (자동 해소) / #4 (mode 2개 = 과한 설계) / #5 (이미 분리됨) 는 product-spec 비스코프.

## Evidence

- **현재 구조**:
  - 진행차단진단 호출부 7개소: `ResumeInterviewOrchestrator.java:102,217,226,231` / `ResumeTurnEventPublisher.java:32` / `FollowUpService.java:86,111`
  - magic number 4개소: `InterrogationModeHandler.java:81 (default answerQuality=2)`, `:115 (currentLevel < 4)`, `ChainStateTracker.java:66 (currentLevel >= 4)`, `:91 (currentLevel > 4)`
  - `InterviewTrack {CS, LANGUAGE, RESUME}` — `Interview.getTrack()`, `ResumeTrackPolicy`, `StandardFollowUpPolicy`, `InterviewTurnPolicy`. WARN 로그에서 `track=RESUME` / `track=STANDARD` 문자열 literal 직접 사용 (enum 미경유).
  - 기존 정책 상수 패턴: `ChainStateTracker.LEVEL_STAY_MAX_TURNS = 2` (entity 내부 `private static final int`).
- **컨벤션 단서**:
  - `backend/.claude/rules/conventions.md` Logging — placeholder + key=value, `@Slf4j` 강제. 본 spec 변경 후에도 룰 만족.
  - `backend/.claude/rules/testing.md` — Domain Unit ≥60%. enum / 상수 = Domain Unit.
  - `.claude/rules/simplicity.md` — "확장성 명시 요구 없으면 1회용" / "200줄 가능한 걸 50줄로". 헬퍼 / Marker / Map 추상화 모두 YAGNI.
- **사용자 발화**:
  - "로그에 너무 힘쓰지마" — byte-level 순서 보장 / 헬퍼 / Map 추상화 폐기 결정 근거.
  - "단일 PR 3건 모두" — 컨텍스트 보존.
- **추정 / 미확인**:
  - `LANGUAGE` 트랙은 `Interview.getTrack()` 미반환. 현 시점 미사용 추정. `logLabel()` 매핑 시 `LANGUAGE → "STANDARD"` (CS 동일 그룹) — 향후 LANGUAGE 활성화 시 재평가.

## Trade-offs

### Option A (채택): 식별자 enum + 명명 상수만 도입 (호출부 직접 `log.warn`)

- 장점:
  - 추가 추상화 0 (헬퍼 / Map / Marker 없음).
  - 호출부 string format 패턴 그대로 — 컨벤션 (`backend/.claude/rules/conventions.md` Logging) 와 동일.
  - reason / track 식별자 추가 시 enum 1곳 수정으로 컴파일러 안전.
  - 테스트 단순 (enum 단위 테스트 + 호출부 회귀).
- 단점:
  - 호출부 7개소가 동일 string format (`"[진행차단진단] interviewId={} track={} ..."`) 보유 — 향후 포맷 자체 변경 시 7곳 수정.
- 사유: 본 작업 목적 = **식별자 타입 안전화**. 포맷 자체 변경 = 미래 요구 미확정 (YAGNI). 헬퍼 도입 시 호출부별 추가 키 (3종) 처리 위해 Map / 오버로드 필요 → 추가 추상화 → simplicity.md 위반.

### Option B (폐기): `TurnProgressDiagnostics` 정적 헬퍼 + `extra` Map

- 장점: 포맷 단일 정의. 향후 포맷 변경 1곳.
- 단점: 호출부별 추가 키 (3개소) 처리 위해 `Map<String, Object>` 또는 메서드 오버로드 필요. Map 순회 순서 보장 / `LinkedHashMap` 강제 / `SequencedMap` 등 추가 결정 발생. 추상화 비용 > 통일 이득.
- 폐기 사유: 사용자 명시 ("로그에 너무 힘쓰지마"). simplicity.md "확장성 명시 요구 없으면 1회용" 일치.

### Option C (폐기): SLF4J Marker 헬퍼

- 장점: 미래 라우팅 / 필터.
- 단점: 현재 Logback `%msg` 패턴만 출력 → Marker 0 효과 (default appender 무시). 실 라우팅 위해 인프라 변경 동반 필요.
- 폐기 사유: YAGNI.

## Architecture

```
[호출부 7개소] — string format 직접
   log.warn("[진행차단진단] interviewId={} track={} stage={} reason={} turnIndex={}",
            interviewId, track.logLabel(), stage, reason.logValue(), turnIndex);
   추가 키 필요 호출부 (3개소) = placeholder 추가 후 동일 패턴
```

### 신규 / 변경 클래스

**1. `domain/interview/entity/BlockReason.java` (신규 enum)**

```java
public enum BlockReason {
    PUBLISH_SKIP("publish-skip"),
    QUESTION_ID_MISSING("questionId-missing"),
    RESPONSE_QUESTION_ID_MISSING("response-questionid-missing"),
    RESPONSE_QUESTION_ID_MISMATCH("response-questionid-mismatch"),
    ANALYZER_SKIP("analyzer-skip"),
    STEP_B_SKIP("step-b-skip");

    private final String value;
    BlockReason(String value) { this.value = value; }
    public String logValue() { return value; }
}
```

**명시 매핑 사유**: 자동 변환 (`name().toLowerCase().replace('_', '-')`) 적용 시 `QUESTION_ID_*` 3종이 `question-id-*` 로 분리 출력 → 운영 grep 출력 (`questionId-missing`, `response-questionid-missing`) 과 불일치. 명시 매핑으로 기존 출력 보존.

| enum 값 | logValue() |
|---|---|
| `PUBLISH_SKIP` | `publish-skip` |
| `QUESTION_ID_MISSING` | `questionId-missing` |
| `RESPONSE_QUESTION_ID_MISSING` | `response-questionid-missing` |
| `RESPONSE_QUESTION_ID_MISMATCH` | `response-questionid-mismatch` |
| `ANALYZER_SKIP` | `analyzer-skip` |
| `STEP_B_SKIP` | `step-b-skip` |

**2. `domain/interview/entity/InterviewTrack.java` (변경)**

```java
public enum InterviewTrack {
    CS, LANGUAGE, RESUME;

    public String logLabel() {
        return this == RESUME ? "RESUME" : "STANDARD";
    }
}
```

- 멤버 변경 X (도메인 영향 0).
- `LANGUAGE → "STANDARD"` 그룹 매핑 (CS 동일).

**3. `domain/resume/entity/ChainStateTracker.java` (변경)**

- `public static final int MAX_LEVEL = 4;` 추가 (기존 `LEVEL_STAY_MAX_TURNS = 2` 패턴 동일).
- `:66` `if (currentLevel >= MAX_LEVEL)` 적용.
- `:91` `return currentLevel > MAX_LEVEL` 적용.
- public 노출 사유: 외부 사용처 (`InterrogationModeHandler:115`) 1곳 — 정책 정수 외부 노출은 entity 의 정상 책임. 메서드 캡슐화 = 단일 호출부에는 과함.

**4. `domain/resume/service/InterrogationModeHandler.java` (변경)**

- `private static final int DEFAULT_ANSWER_QUALITY = 2;` (handler 내부 상수).
- `:81` `int answerQuality = analysis != null ? analysis.answerQuality() : DEFAULT_ANSWER_QUALITY;`
- `:115` `if (currentLevel < ChainStateTracker.MAX_LEVEL)` 적용.

### 호출부 7개소 변경

| 위치 | 기존 | 변경 후 |
|---|---|---|
| `Orchestrator.java:102` | `log.warn("[진행차단진단] interviewId={} track=RESUME stage={} reason=publish-skip turnIndex={}", interviewId, currentMode.name().toLowerCase(), turnIndex)` | `log.warn("[진행차단진단] interviewId={} track={} stage={} reason={} turnIndex={}", interviewId, InterviewTrack.RESUME.logLabel(), currentMode.name().toLowerCase(), BlockReason.PUBLISH_SKIP.logValue(), turnIndex)` |
| `Orchestrator.java:217` | `... reason=questionId-missing ... type={}` | 동일 패턴 + `BlockReason.QUESTION_ID_MISSING.logValue()` + `type={}` placeholder 유지 |
| `Orchestrator.java:226` | `... reason=response-questionid-missing handlerQuestionId={}` | 동일 패턴 + `BlockReason.RESPONSE_QUESTION_ID_MISSING.logValue()` + `handlerQuestionId={}` placeholder 유지 |
| `Orchestrator.java:231` | `... reason=response-questionid-mismatch handlerQuestionId={} responseQuestionId={}` | 동일 패턴 + `BlockReason.RESPONSE_QUESTION_ID_MISMATCH.logValue()` + 2개 placeholder 유지 |
| `Publisher.java:32` | `... track=RESUME stage={} reason=questionId-missing turnIndex={}` | `InterviewTrack.RESUME.logLabel()` + `BlockReason.QUESTION_ID_MISSING.logValue()` |
| `FollowUpService.java:86` | `... track=STANDARD stage=standard-followup reason=analyzer-skip turnIndex={}` | `InterviewTrack.CS.logLabel()` + `BlockReason.ANALYZER_SKIP.logValue()` (stage literal `"standard-followup"` 유지) |
| `FollowUpService.java:111` | `... reason=step-b-skip turnIndex={}` | `InterviewTrack.CS.logLabel()` + `BlockReason.STEP_B_SKIP.logValue()` |

**stage**: enum 화 X. 3종 (`playground`, `interrogation`, `standard-followup`) — `ResumeMode.name().toLowerCase()` 자동 매핑 + `FollowUpService` literal `"standard-followup"`. enum 도입 = 과한 추상화.

**InterviewTrack 인스턴스**: 호출 컨텍스트 자체가 트랙 명확 → enum literal 직접 전달 (`InterviewTrack.RESUME` / `InterviewTrack.CS`).

## NF (Non-Functional) 11개

| NF | 결정 | 근거 |
|---|---|---|
| 영향 범위 | BE only (`domain/interview` + `domain/resume`) | grep 검증. FE / lambda 영향 0. |
| 정합성 | 키 스키마 동등 (운영 grep 호환) | 키=값 쌍 모두 등장 검증. 출력 순서는 placeholder 직접 작성 → 호출부 단위로 결정적. |
| 실시간성 | 변경 전후 동등 | 동기 `log.warn` 그대로. |
| 부하 | 영향 0 | 호출 횟수 / TPS 동일. |
| 동시성 | 무관 | enum / 상수 = 불변. 락 / 공유 상태 추가 0. |
| 마이그레이션 | DB 변경 0 | Flyway 불필요. |
| 외부 의존 | 없음 | SLF4J 외 신규 의존 0. |
| 보안 | A09 영향 0 | 출력 키·값 동등. PII 추가 0. `.claude/rules/security.md` 위반 0. |
| 관찰성 | **본 spec 핵심** | 기존 grep 패턴 (`stage=playground`, `reason=publish-skip`, `track=RESUME`) 보존. |
| 롤백 | revert PR 1회 | DB 0 / flag 0 / schema 0. |
| 검증 | Domain Unit + Service Integration | enum / 상수 = Domain Unit / 호출부 회귀 = Service Integration 1건. `backend/.claude/rules/testing.md` 매핑. |

## Data Model

**변경 없음.** Flyway 마이그레이션 불필요.

## API Contract

**변경 없음.** 외부 endpoint / DTO / 응답 영향 0.

## Verification (완료 판정)

### Domain Unit

- [ ] `BlockReasonTest`: 6종 `logValue()` 매핑 assertion (enum 값 ↔ 출력 문자열).
- [ ] `InterviewTrackTest`: `RESUME.logLabel() == "RESUME"`, `CS.logLabel() == "STANDARD"`, `LANGUAGE.logLabel() == "STANDARD"`.
- [ ] `ChainStateTrackerTest` (회귀): `levelUp()` `MAX_LEVEL` 도달 후 호출 시 유지 / `isChainComplete()` `> MAX_LEVEL` 조건 보존.

### Service Integration

- [ ] 카테고리: `ServiceIntegrationSupport` (`@SpringBootTest` + TRUNCATE per test).
- [ ] 신규 1건: `[진행차단진단]` 시나리오 1종 트리거 (publish-skip 추천 — 호출 경로 단순) + LogCaptor 또는 `OutputCaptureExtension` 으로 메시지 캡처 + 키 5개 (`interviewId=`, `track=RESUME`, `stage=playground`, `reason=publish-skip`, `turnIndex=`) 등장 assertion (순서 무관).
- [ ] 기존 `ResumeInterviewOrchestratorTest` / `FollowUpServiceTest` 통합 테스트 회귀 통과.

### 빌드 / 린트

- [ ] `./gradlew clean build` 통과.
- [ ] `./gradlew test` 통과.
- [ ] 잔존 grep 0건:
  - `grep -rn '"publish-skip"\|"questionId-missing"\|"response-questionid-missing"\|"response-questionid-mismatch"\|"analyzer-skip"\|"step-b-skip"' backend/src/main/java` → enum 정의부 외 0건.
  - `grep -rn '"track=RESUME"\|"track=STANDARD"\|track=RESUME\|track=STANDARD' backend/src/main/java` → 호출부 patten string 외 발생 케이스 0 (placeholder `track={}` 만 잔존 OK).
  - `grep -rn 'currentLevel.*[<>=].*4\b' backend/src/main/java` → `MAX_LEVEL` 외 0건.
  - `grep -rn 'answerQuality().*:.*2\b' backend/src/main/java` → `DEFAULT_ANSWER_QUALITY` 외 0건.

### 관찰 가능 동작

- [ ] dev 환경 진행차단진단 시나리오 1건 트리거 시 로그 출력 키 스키마 (`interviewId`, `track`, `stage`, `reason`, `turnIndex`) 등장 + 값 변경 전과 동일 (운영 grep 패턴 호환).

### 회귀 체크

- [ ] `domain/resume` 트랙 통합 테스트 (Playground · Interrogation 전이 / questionId 검증 / publish 흐름) 통과.
- [ ] `domain/interview FollowUpService` (CS 트랙 standard-followup) 통합 테스트 통과.

## Pre / Post State

### Pre (현재)

- 7개소 직접 `log.warn("[진행차단진단] ...")` string format. track / reason 문자열 literal.
- `InterrogationModeHandler.java:81,115` raw int `2`, `4`.
- `ChainStateTracker.java:66,91` raw int `4`.
- `InterviewTrack` enum 멤버 `{CS, LANGUAGE, RESUME}` — log 매핑 부재.

### Post (구현 후)

- 7개소 `log.warn` 호출 그대로 + reason / track literal 부분만 enum 호출 (`BlockReason.X.logValue()` / `InterviewTrack.X.logLabel()`).
- `BlockReason` enum 6종 + 명시 매핑 (`logValue()`).
- `InterviewTrack.logLabel()` 매핑 메서드 (멤버 변경 X).
- `ChainStateTracker.MAX_LEVEL` public 상수 + 내부 / 외부 호출부 통일.
- `InterrogationModeHandler.DEFAULT_ANSWER_QUALITY` private 상수.
- 로그 출력 키 스키마 / 값 = 변경 전 동등 (운영 grep 호환).

### Diff 요약

| 파일 | 변경 |
|---|---|
| `BlockReason.java` (신규) | + enum 6종 + 명시 매핑 |
| `InterviewTrack.java` | + `logLabel()` 메서드 |
| `ChainStateTracker.java` | + public `MAX_LEVEL` 상수, `:66 :91` 적용 |
| `InterrogationModeHandler.java` | + `DEFAULT_ANSWER_QUALITY` 상수, `:81 :115` 적용 |
| `ResumeInterviewOrchestrator.java` | `:102 :217 :226 :231` enum 호출 적용 |
| `ResumeTurnEventPublisher.java` | `:32` enum 호출 적용 |
| `FollowUpService.java` | `:86 :111` enum 호출 적용 |
| `BlockReasonTest.java` (신규) | + 단위 테스트 |
| `InterviewTrackTest.java` (신규 또는 기존) | + `logLabel()` 케이스 |
| `ChainStateTrackerTest.java` (기존) | + `MAX_LEVEL` 회귀 케이스 |
| 통합 테스트 (`ResumeInterviewOrchestratorTest` 또는 동등) | + `[진행차단진단]` 로그 캡처 1건 |

## 위험 / 마이그레이션 / 롤백

- **위험**:
  - `BlockReason.logValue()` 명시 매핑 오타 → 운영 grep 깨짐. **완화**: `BlockReasonTest` 6종 매핑 assertion.
  - `ChainStateTracker.MAX_LEVEL` public 노출 → 외부 침투 위험. **완화**: 사용처 `InterrogationModeHandler` 1곳만. 정책 정수 외부 노출 정상.
- **마이그레이션 전략**: 없음. 코드 리팩토링 only.
- **롤백 시나리오**: revert PR 1회 (DB 0 / flag 0 / schema 0).

## 분기 결정

- [x] **단일 영역 → `implement.md` 1개** (BE only)
- [ ] BE+FE 동시 (해당 없음)
- [ ] BE 선행 강제 (해당 없음)

**PR 분리 정책**: 사용자 결정 = **단일 PR 3건 모두 묶음**.

## 참고

- product-spec: `docs/plans/430-warn-log-helper/product-spec.md`
- Issue: #430 (Epic), PR #429 (`Issue #423` IntentClassifier 제거 — 머지)
- 관련 plan: `docs/plans/423-intent-classifier-removal/`
- 컨벤션: `backend/.claude/rules/conventions.md`, `testing.md`, `.claude/rules/simplicity.md`
- 템플릿: `docs/plans/_templates/tech-spec.md`
