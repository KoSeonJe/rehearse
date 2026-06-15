# Implement (Backend) — Resume 트랙 WRAP_UP 모드 제거

> **작성자**: backend agent
> **답하는 질문**: BE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★

---

## Phase 0: API Contract 확인 + 선행 의존

`tech-spec.md#api-contract` 의 schema 확정 + 사용자 명시 승인 완료 (3차 리뷰 PASS).

- [x] Endpoint = 기존 `POST /api/v1/interviews/{id}/follow-up` (변경 없음)
- [x] Request schema 변경 = `FollowUpRequest.terminate: boolean` 1필드 추가 (default false)
- [x] Response schema 변경 없음 = 종료 케이스 = `followUpExhausted=true` 재사용
- [x] Error 매핑 변경 없음

### 선행 의존 (Blocking)

- [ ] **plan 423 (intent classifier removal) 머지 완료** — `ResumeInterviewOrchestrator.processUserTurn` 본문 conflict 영역 baseline 변동. 423 머지 → 424 rebase 후 본 implement 진입.

미완 → 즉시 STOP. tech-spec 갱신 + 사용자 승인 재요청.

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 예상 PR | 의존 |
|-------|------|------|--------|------|
| 1 | (스킵 — 운영 SQL / V46 모두 불필요) | — | — | — |
| 2 | enum + 회고 산출물 9종 제거 + 설정 키 정리 | `backend` | #N (동일 PR) | Phase 0 |
| 3 | FollowUpRequest.terminate + Orchestrator / Policy 재작성 | `backend` | #N (동일 PR) | Phase 2 |
| 4 | 통합 테스트 + 정적 grep + docs 정리 | `backend` | #N (동일 PR) | Phase 3 |

> 단일 PR `[BE] refactor: Resume 트랙 WRAP_UP 모드 제거` — Phase 2/3/4 를 작업 단위로 커밋 분할 후 1 PR 묶음. Flyway 신규 마이그레이션 0건 (Amendment — V42 가 이미 chk_question_track_meta_v2 + chain_*/project_id DROP). 운영 SQL cleanup 불필요 — RESUME_WRAP_UP row dev 환경에만 존재 / prod 부재.

---

## Phase 1: (스킵 — 운영 SQL / V46 모두 불필요)

RESUME_WRAP_UP row 가 dev 환경에만 존재 / prod 부재 확인. 운영 cleanup 불필요. V46 / V47 폐기 (Amendment 동일 사유).

---

## Phase 2: enum + 회고 산출물 9종 제거 + 설정 키 정리

- **구현**: `backend` — Resume WRAP_UP 관련 enum / handler / prompt builder / focus / fallback / yml 모두 제거

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ResumeMode.java` — `WRAP_UP` enum 값 제거 (2종으로 축소)
- `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionType.java` — `RESUME_WRAP_UP` enum 값 제거 (5종으로 축소). `Question.resume(...)` factory 분기에서 RESUME_WRAP_UP 참조 정리
- `backend/src/main/java/com/rehearse/api/domain/resume/service/WrapUpModeHandler.java` — **파일 삭제**
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeWrapUpPromptBuilder.java` — **파일 삭제** (record `WrapUpResult` 포함)
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeQuestionResultGenerator.java` — `generateWrapUp` 메서드 + `wrapUpPromptBuilder` 의존 + `MODE_WRAP_UP` 상수 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/context/focus/FocusHints.java` — `ResumeWrapUpHints` record + sealed permits 항목 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/context/focus/FocusLayer.java` — `CAP_RESUME_WRAP_UP` 상수 + `buildResumeWrapUp` 메서드 + case 매핑 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/skeleton/SkeletonCallType.java` — `RESUME_WRAP_UP` enum 값 + 프롬프트 텍스트 제거
- `backend/src/main/java/com/rehearse/api/domain/resume/fallback/ResumeFallbackQuestions.java` — `WRAP_UP` 상수 제거
- `backend/src/main/java/com/rehearse/api/domain/resume/fallback/ResumeFallbackModelAnswers.java` — `WRAP_UP` 상수 제거
- `backend/src/main/resources/application.yml` — `rehearse.resume-track.wrap-up-threshold-min: 2` 키 + 부모 path 정리 (다른 키 잔존 시 보존)
- `backend/src/main/resources/application-*.yml` — 동일 키 잔존 시 동일 정리 (test/local/dev/prod 환경별 yml grep 후 처리)

> 정확한 path / 클래스 위치는 tech-spec Evidence 매핑 + Phase 진입 시점 grep `RESUME_WRAP_UP|WrapUp|wrap-up-threshold-min|ResumeWrapUp` 으로 재확정.

### 핵심 로직

1. enum 값 삭제 → 컴파일 에러로 사용처 자동 식별 → 한꺼번에 정리.
2. sealed `FocusHints` permits 누락 시 컴파일 차단 → 즉시 식별.
3. `Question.resume(...)` factory 의 RESUME_WRAP_UP 분기 제거. 기존 호출처는 Phase 3 에서 정리되므로 일시적 컴파일 에러 발생 가능 — Phase 2/3 동일 PR 내 연쇄 처리.
4. yml 키 삭제 → `@Value("${rehearse.resume-track.wrap-up-threshold-min}")` 등 주입 부재 → Phase 3 의 `ResumeModeTransitionPolicy` 정리와 동일 PR 묶음.

### 의존

- 선행: Phase 0 (Flyway 신규 0건 — Phase 1 BE PR 비스코프)
- 외부: 없음

### Verification

- [ ] `./gradlew compileJava` 통과 (Phase 3 와 묶어서 통과 가능 — 본 Phase 단독은 일시 컴파일 에러 허용)
- [ ] grep `RESUME_WRAP_UP|WrapUp|ResumeWrapUp` backend/src/main/java → 0건
- [ ] grep `wrap-up-threshold-min|wrapUpThresholdMin` backend/src/main → 0건

### 커밋 메시지

```
refactor(BE): Resume WRAP_UP enum/handler/prompt/focus/fallback/yml 일괄 제거
```

---

## Phase 3: FollowUpRequest.terminate + Orchestrator / Policy 재작성

- **구현**: `backend` — terminate 신호 분기 + WRAP_UP 분기 제거 + Policy 정리

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpRequest.java` — `private boolean terminate` 필드 추가 (기본값 false). `@Getter @NoArgsConstructor` 패턴 그대로 (Jackson `isTerminate()` getter / field reflection 매핑).
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java` — `processUserTurn` 재작성:
  - `wrapUpHandler` 의존 / 호출 제거
  - `advanceToWrapUpIfDue` 호출 제거
  - 분기 7번 `terminate==true` 처리 추가 — `turnAnalysisPipeline.analyze` 결과 활용 (분석 자체 수행) + 신규 question INSERT skip + `terminateResponse()` 반환 (`followUpExhausted=true / skip=true / presentToUser=false`)
  - `dispatchByMode` switch 에서 `WRAP_UP` case 제거 (PLAYGROUND / INTERROGATION 만)
  - 로그 분리: `log.info("[ResumeOrchestrator] FE-signaled terminate: interviewId={}, lastQuestionAnalyzed=true", id)` / `log.warn("[ResumeOrchestrator] hard timeout backstop: interviewId={}", id)`
  - **turnEventPublisher.publish 호출 여부**: tech-spec 결정 보류 항목. implement 시 listener (`ResumeTurnEventPublisher` consumer) 의 questionId null 처리 동작 grep + Read 점검 후 결정. 결정 결과는 본 implement-be.md 하단 `## 결정 로그` 섹션에 1줄 기록.
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeModeTransitionPolicy.java` — `advanceToWrapUpIfDue` 메서드 / `wrapUpThresholdMin` 필드 / 해당 `@Value` 제거. `isHardTimeoutExceeded` 유지.

### 핵심 로직

```
processUserTurn(request, ctx):
  1. clockWatcher.markStart(ctx)
  2. analysis = turnAnalysisPipeline.analyze(request, ctx)
  3. (423 머지 결과) non-answer intent 분기 처리
  4. answerAnalysis = analysis.answer / runtimeState = runtimeStateCache.get(ctx)
  5. remaining = clockWatcher.remainingMinutes(ctx)
  6. if modeTransitionPolicy.isHardTimeoutExceeded(ctx, remaining):
       log.warn("[ResumeOrchestrator] hard timeout backstop: ...")
       return hardTimeoutResponse()
  7. if request.isTerminate():
       publish-or-skip per implement 결정
       log.info("[ResumeOrchestrator] FE-signaled terminate: ...")
       return terminateResponse()
  8. dispatchByMode(runtimeState.mode, ...)  // PLAYGROUND | INTERROGATION
  9. validateQuestionId / turnEventPublisher.publish (기존)
 10. return
```

### 의존

- 선행: Phase 2 (enum / 산출물 제거 → 컴파일 일시 에러 해소)
- 외부: 없음

### Verification

- [ ] `./gradlew compileJava` 통과 (Phase 1+2+3 누적 후 그린)
- [ ] DTO 직렬화 테스트 — `ObjectMapper.readValue("{\"terminate\":true}", FollowUpRequest.class).isTerminate() == true`

### 커밋 메시지

```
feat(BE): FollowUpRequest.terminate + Resume Orchestrator FSM 2단계 단순화
```

---

## Phase 4: 통합 테스트 + 정적 grep + docs 정리

- **구현**: `backend` — Service Integration / Repository (V46 Testcontainers) / Domain Unit + 정적 검증 + docs 영역 RESUME_WRAP_UP 잔존 제거

### 변경 파일

- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestratorIntegrationTest.java` — 신규 또는 기존 확장:
  - 정상 다중 턴 (PLAYGROUND → INTERROGATION 전이 / 회고 질문 미발생 / RESUME_WRAP_UP INSERT 0)
  - terminate=true 경로 (turnAnalysisPipeline 호출 1회 검증 / 신규 question INSERT 0 / 응답 followUpExhausted=true & skip=true & presentToUser=false)
  - hard timeout backstop 경로 (terminate=false + 시간 초과 → 종료 응답 / question INSERT 0)
  - 멱등성: terminate=true 후 동일 interviewId 재요청 → 기존 follow-up 정책 echo 회귀
  - 첫 턴 terminate (Playground opener 답변 케이스): turnAnalysisPipeline 호출 1회 + INSERT 0 + 예외 미발생
  - Playground 진입 직후 답변 0건 + terminate=true: 적재 없음 + INSERT 0 (BE 방어)
- `backend/src/test/java/com/rehearse/api/domain/resume/entity/ResumeModeTest.java` — enum 2종 검증
- `backend/src/test/java/com/rehearse/api/domain/question/entity/QuestionTypeTest.java` — RESUME_WRAP_UP 부재 검증
- `backend/src/test/java/com/rehearse/api/domain/interview/dto/FollowUpRequestTest.java` — terminate Jackson deserialize 검증 (default false / 명시 true / 명시 false)
- `docs/domain/resume/glossary.md` / `schema.md` / `api/process-user-turn.md` — RESUME_WRAP_UP / WRAP_UP / wrap-up-threshold-min 관련 단락·다이어그램 제거
- `docs/domain/question/glossary.md` / `schema.md` — RESUME_WRAP_UP 행 제거
- `docs/domain/interview/api/follow-up.md` — terminate 필드 추가 + WRAP_UP 종료 단락 제거
- `docs/domain/interview/runtime-state-and-context-layers.md` — WRAP_UP 단계 다이어그램 / 표 정리

### 핵심 로직

- testing.md 정책 준수: `ServiceIntegrationSupport` + Testcontainers MySQL + msw 류 모킹 X (BE 외부 = AI clients 만 mock).
- grep 검증 셸 사용:
  ```
  grep -rEn "RESUME_WRAP_UP|WrapUp|wrap-up-threshold-min|wrapUpThresholdMin|ResumeWrapUp" backend/src
  grep -rEn "RESUME_WRAP_UP|WRAP_UP|wrap-up-threshold-min" docs/domain
  ```
  결과 0건 = green.

### 의존

- 선행: Phase 3 (코드 그린)
- 외부: Testcontainers 데몬 (Docker Desktop)

### Verification

- [ ] `./gradlew test --tests "com.rehearse.api.domain.resume.*"`
- [ ] `./gradlew test --tests "com.rehearse.api.domain.interview.service.FollowUpServiceTest"`
- [ ] `./gradlew test --tests "com.rehearse.api.infra.ai.context.*"` (FocusLayer / SkeletonCallType 영향)
- [ ] `./gradlew build` (전체)
- [ ] grep `RESUME_WRAP_UP|WrapUp|wrap-up-threshold-min|wrapUpThresholdMin|ResumeWrapUp` backend/src → 0건
- [ ] grep `RESUME_WRAP_UP|WRAP_UP|wrap-up-threshold-min` docs/domain → 0건
- [ ] dev 배포 후 E2E 1회: Resume 시작 → 다중 턴 → duration 초과 후 답변 제출 → 종료 응답 → 로그에 `FE-signaled terminate` / `hard timeout backstop` 분리 확인

### 커밋 메시지

```
test(BE): Resume Orchestrator terminate / hard timeout / 첫 턴 / 멱등 통합 테스트
```

(docs 정리는 별도 커밋)

```
docs(BE): Resume / Question / Interview 도메인 문서에서 WRAP_UP 단락 제거
```

---

## FE 와 통합 시점

- BE 머지 직후 FE 측 알림 (Issue #424 댓글 + Slack)
- FE 가 `terminate` 신호 전송 + QuestionType 유니언 정리
- FE Phase 3 통합 테스트 단계에서 BE dev 배포 응답 200 / 종료 페이즈 진입 회귀 확인

## 통합 Verification

- [ ] tech-spec.md Verification 섹션 (Service Integration 7항목 / Domain Unit 2항목 / Repository 2항목 / FE Integration 3항목 / 정적 grep 3항목 / 빌드 2항목 / 관찰 가능 2항목 / 회귀 4항목) 통과
- [ ] FE 통합 후 Resume 트랙 회귀 (전체 플로우)

## 결정 로그 (implement 진입 후 갱신)

- [x] 시퀀스 7번 `turnEventPublisher.publish` 호출 여부 — **B (skip) 채택**. terminate 분기에서 publish 미호출. 사유: terminate 는 의도된 종료인데 publisher 의 questionId-null 분기는 `[진행차단진단] questionId-missing` warn 로그를 찍어 운영자에게 비정상 시그널로 오인됨 (RubricScoringEventListener 도 questionId null event 시 IllegalStateException 던지므로 어차피 score persist 미발생). 명시 분기로 publish 자체를 안 부르는 편이 알림 정확도 + 의도 표현 모두 우월.

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-backend` 실행 (구현 완료 직후 — 메인 세션 책임)
- [ ] BE+FE 동시 작업 시 `code-reviewer-frontend` 와 **병렬** 호출 (단일 메시지 multiple tool_use)
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec `## Pre / Post State` 기준)
