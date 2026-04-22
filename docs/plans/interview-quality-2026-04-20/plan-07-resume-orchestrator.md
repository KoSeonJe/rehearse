# Plan 07: Resume Orchestrator (Phase 3 — Playground + Interrogation)

> 상태: Draft
> 작성일: 2026-04-20
> 주차: W3
> 원본: `docs/todo/2026-04-20/06-resume-track.md` (Phase 3)
> 의존: plan-01, plan-02, plan-04, plan-05, plan-06

## Why

CS/언어 면접과 이력서 면접은 게임의 룰이 근본적으로 다르다(지식 검증 vs 사실+의사결정 심문, 정답 있음 vs 없음, 수평 확장 vs 수직 파고들기). 현재 하나의 follow-up 프롬프트로 처리하면 이력서 파트가 특히 손해. 실제 면접 흐름을 따라 **놀이터(자기소개) → 심문(chain 4단계 파고들기)** 2단계를 도입하고, L1→L2→L3→L4 chain 진행을 `ChainStateTracker`로 통제하면 사용자가 "진짜 면접관이 파고드는" 느낌을 받는다.

**사실 검증 flag(이력서 vs 답변 모순 탐지)는 Out of Scope** — 정밀도 관리 부담 큼, MVP 이후.

### REMEDIATION 반영 (critic: TODO 원본 vs 본 plan 모순 해소)
- `resume-chain-interrogator.txt` JSON 스키마에서 `fact_check_flag`, `fact_check_note` **필드 삭제** (TODO 원본 06 문서에는 있지만 본 스프린트 범위 아님)
- 프롬프트 본문에 "본 버전에서는 이력서와 답변 간 사실 불일치를 감지/기록하지 않습니다. 단순히 chain 진행만 결정하세요" 명시
- `ResumeInterviewOrchestrator` 도 `fact_check_flag` 관련 분기 없음 — 추후 별도 plan에서 추가

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/prompts/template/resume/resume-playground-opener.txt` | 신규 |
| `backend/src/main/resources/prompts/template/resume/resume-playground-responder.txt` | 신규. 전환 판단 포함 |
| `backend/src/main/resources/prompts/template/resume/resume-chain-interrogator.txt` | 신규. Step B 변형 (LEVEL_UP/STAY/CHAIN_SWITCH 결정) |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeChainInterrogatorPromptBuilder.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/resume/ResumeInterviewOrchestrator.java` | 신규. 메인 진입점 |
| `backend/src/main/java/com/rehearse/api/domain/resume/PlaygroundModeHandler.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/resume/InterrogationModeHandler.java` | 신규 |
| `backend/src/main/java/com/rehearse/api/domain/resume/ChainStateTracker.java` | 신규. 세션 내 상태 |
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/ResumeMode.java` | 신규 enum (PLAYGROUND/INTERROGATION/**WRAP_UP**) |
| `backend/src/main/java/com/rehearse/api/domain/resume/WrapUpModeHandler.java` | **신규 (2026-04-22)**. WRAP_UP 모드 질문 생성 담당 |
| `backend/src/main/java/com/rehearse/api/domain/resume/ClockWatcher.java` | **신규 (2026-04-22)**. 세션 시작 시각 기록 + `remaining_time` 계산 |
| `backend/src/main/resources/prompts/template/resume/resume-wrap-up.txt` | **신규 (2026-04-22)**. WRAP_UP 회고 질문 프롬프트 |
| `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeWrapUpPromptBuilder.java` | **신규 (2026-04-22)** |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` | **수정 (기존 — `InterviewTurnService` 없음)**. Interview에 `resumeSkeletonId`가 있으면 `ResumeInterviewOrchestrator`로 위임 |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewCreationService.java` | **수정 (2026-04-22)**. Resume Exclusivity validator — `resumeFile != null && interviewTypes ≠ {RESUME_BASED}` 면 400 거부 |
| `backend/src/main/java/com/rehearse/api/global/error/...ControllerAdvice` | **수정 (2026-04-22)**. `RESUME_EXCLUSIVITY_VIOLATION` 에러 코드 매핑 |
| `frontend/src/hooks/use-interview-setup.ts` | **수정 (2026-04-22)**. RESUME_BASED 토글 시 다른 types 자동 clear + 활성 시 disabled flag 반환 |
| `frontend/src/components/setup/step-interview-type.tsx` | **수정 (2026-04-22)**. RESUME_BASED 활성 시 다른 카드 disabled 스타일 + 안내 배너 |

## 상세

### 처리 흐름
```java
// ResumeInterviewOrchestrator.processUserTurn()
IntentResult intent = intentClassifier.classify(...);                 // plan-01 재사용
if (intent.isNotAnswer()) return handleNonAnswerIntent(...);

AnswerAnalysis analysis = answerAnalyzer.analyze(...);                 // plan-02 재사용
session.recordAnalysis(analysis);                                      // M3/plan-08용

// Dynamic Pacing: 매 턴 시작 시 시간 체크 후 WRAP_UP 전이 판단
if (clockWatcher.remainingMinutes(session) <= wrapUpThresholdMin
        && session.getResumeMode() != ResumeMode.WRAP_UP) {
    session.transitionTo(ResumeMode.WRAP_UP);
}

return switch (session.getResumeMode()) {
  case PLAYGROUND     -> playgroundHandler.handle(session, userUtterance, analysis);
  case INTERROGATION  -> interrogationHandler.handle(session, userUtterance, analysis);
  case WRAP_UP        -> wrapUpHandler.handle(session, userUtterance, analysis);
};
```

### Playground 모드
- **금지**: 왜/원리/타 프로젝트 점프
- **전환 조건** (4개 중 2개 이상 만족 시 `should_switch_to_interrogation: true`):
  a. expected_claims 60% 이상 언급됨
  b. 사용자 누적 발화 ≥ 300자
  c. 종결 시그널("그래서", "결론적으로") 감지
  d. 놀이터 누적 턴 ≥ 3 (하드 리밋)

### Interrogation 모드 (Step B 변형)
- **결정 트리**:
  - `LEVEL_UP`: answer_quality ≥ 3 AND current_level < 4
  - `LEVEL_STAY`: answer_quality ≤ 2 (단, 같은 레벨 2턴 초과 금지)
  - `CHAIN_SWITCH`: L3-L4 충분히 커버 OR 시간 배분상 다음 chain OR 명확한 "모릅니다"
- **질문 작문**: Chain의 원본 level 질문을 사용자 답변 키워드로 각색

### WRAP_UP 모드 + Dynamic Pacing (2026-04-22)

**FSM 확장**: 기존 2단계(PLAYGROUND → INTERROGATION) → 3단계.

```
[PLAYGROUND] ──(전환 4조건 중 2개)──▶ [INTERROGATION] ──(remaining_time ≤ threshold)──▶ [WRAP_UP] ──▶ (세션 종료)
```

**전이 조건**:
- `INTERROGATION → WRAP_UP`: `clockWatcher.remainingMinutes(session) ≤ rehearse.features.resume-track.wrap-up-threshold-min` (기본 2분)
- `PLAYGROUND → WRAP_UP`: 드물지만 사용자가 너무 느려 Playground 단계에서 시간 소진 시에도 직접 전이 허용

**WRAP_UP 모드 규칙**:
- **금지**: 새 chain 시작, LEVEL_UP (다음 레벨 올라가기), CHAIN_SWITCH 로 backup_chains 꺼내기
- **허용**: 현재 진행 중 chain 완결 (L2 중간이면 L2 완결까지), 회고 질문 1회 추가
- **종료**: 사용자가 WRAP_UP 질문에 답변 시 자연 종료. `remaining_time ≤ 0` 이면 강제 종료 + plan-09 Synthesizer 트리거

**WRAP_UP 질문 pool (resume-wrap-up.txt)** — 사용자 세션 맥락 기반 LLM 각색 1개 생성:
- "오늘 얘기 나눈 것 중 가장 어려웠던 부분이 뭐였어요?"
- "다음 면접에서 더 준비해오고 싶은 주제 있으세요?"
- "마지막으로 덧붙이고 싶은 내용 편하게 말씀해주세요"

**엣지 케이스**:
| 상황 | 처리 |
|---|---|
| 사용자 느려서 L1 에서 세션 끝 | WRAP_UP 에서 "못 다룬 주제는 다음에" 안내. 완료된 턴 기준으로 plan-09 피드백 생성 |
| 사용자 빨라서 모든 primary chain 끝 + 10분 남음 | `backup_chains` 자동 소비 (INTERROGATION 유지). 그마저 끝나면 조기 WRAP_UP 전이 |
| WRAP_UP 중 사용자 답변 10분 이상 지연 | hard timeout 으로 강제 종료 후 피드백 생성 |

**Feature Flag**:
```yaml
rehearse:
  features:
    resume-track:
      wrap-up-threshold-min: 2   # INTERROGATION → WRAP_UP 전이 기준
      hard-timeout-min: 10       # WRAP_UP 중 사용자 무응답 시 강제 종료
```

**ClockWatcher 컴포넌트**:
- 세션 시작 시각 기록 (`InterviewRuntimeState.startedAt` 추가)
- `remainingMinutes(session)` = `session.getDurationMinutes() - elapsed`
- 매 턴 시작 시 Orchestrator 가 호출. stateless util + `InterviewRuntimeStateStore` 의존

**Why Dynamic Pacing?** (plan-06 참조)
Planner 가 duration 별 chain 을 잘라놓는 Budgeted 방식은 사용자 페이스 적응 불가. 최대로 준비하고 Orchestrator 가 WRAP_UP 모드로 자연 종료하는 게 UX + 복잡도 모두 유리.

### Resume Exclusivity Rule (2026-04-22)

**규칙**:
```
Interview 생성 요청에 resumeFile 또는 interviewTypes 내 RESUME_BASED 가
포함되면, interviewTypes 는 반드시 {RESUME_BASED} 단일값이어야 한다.
위반 시 BE 는 400 Bad Request 를 반환한다.
```

**Why**: Interrogation chain (L1→L2→L3→L4) 은 연속 맥락 전제. CS 질문이 중간에 끼면 plan-08 D10 Chain Depth 채점 불가 + FSM 붕괴 + LLM 맥락 리셋 비용. "조용히 동작 이상" 을 방지하고 제품 계약을 명확히 한다.

**Enforcement (Defense in depth)**:

**L1 (FE, UX 차단)**:
- RESUME_BASED 체크 시 다른 카드 disabled + 기존 선택 자동 해제
- 이미 다른 type 선택 상태에서 RESUME_BASED 클릭 시 확인 후 모두 해제
- 안내 배너: "이력서 면접은 이력서 전용 모드로 진행됩니다"
- 수정 파일: `frontend/src/hooks/use-interview-setup.ts`, `frontend/src/components/setup/step-interview-type.tsx`

**L2 (BE, 최종 계약)**:
- `InterviewCreationService` 진입 validator
- `resumeFile != null && interviewTypes != {RESUME_BASED}` → `IllegalArgumentException`
- ControllerAdvice 매핑: 400 + error code `RESUME_EXCLUSIVITY_VIOLATION` + 한국어 메시지
- 수정 파일: `backend/.../InterviewCreationService.java`, `backend/.../ControllerAdvice`

**테스트 계약**:
| 입력 | 기대 |
|---|---|
| `resumeFile != null + types={CS_FUNDAMENTAL, RESUME_BASED}` | 400 + `RESUME_EXCLUSIVITY_VIOLATION` |
| `resumeFile != null + types={RESUME_BASED}` 단독 | 200 정상 생성 |
| `resumeFile == null + types={RESUME_BASED}` | 400 (이력서 없이 RESUME_BASED 불가, 기존 FE 검증도 유지) |
| `resumeFile == null + types={CS_FUNDAMENTAL, BEHAVIORAL}` | 200 (일반 혼합은 허용) |

### ChainStateTracker
```java
class ChainStateTracker {
  Map<String, ChainProgress> activeChains;
  String currentChainId;
  int currentLevel;
  List<ChainProgress> completedChains;

  NextAction decide(Project current, String userAnswer, AnswerAnalysis analysis,
                    InterviewPlan plan, int remainingTimeMin);
}
```

### 재사용 확인
- `IntentClassifier` (plan-01) — CLARIFY/GIVE_UP 분기 그대로
- `AnswerAnalyzer` (plan-02) — claims/unstated_assumptions 추출 그대로
- `InterviewContextBuilder` (plan-04) — Resume Skeleton을 L1에, ChainState를 L2에, 활성 chain을 L4에 주입

### plan-08 Rubric Family 연계 (D9/D10 채점 데이터 전달)
Orchestrator가 턴 종료 hook에서 `RubricScorer.score()` 호출 시 아래 데이터 추가 전달:
- `resumeMode` (PLAYGROUND | INTERROGATION) → rubric의 mode-aware per_turn_rules 발동
- `currentChainLevel` (1~4) → D10 Chain Depth 점수 매핑 (LLM 추정 아닌 확정값)
- `ResumeSkeleton` 의 현재 project/claim 섹션 → D9 Factual Consistency 채점 시 프롬프트에 주입(이력서 원문 vs 답변 일치 판정)

**D9 Factual Consistency는 Rubric에서 점수만 매김. Chain Interrogator는 분기 flag를 내지 않음** — `fact_check_flag` 삭제 결정 유지. 사용자에게 "불일치" 알림은 plan-09 Synthesizer의 Gap 섹션에서 D9 score ≤ 2 턴을 관찰 인용으로 제시(이력서 vs 답변 대조 구절).

### Feature Flag
```yaml
rehearse:
  features:
    resume-track:
      enabled: true
      playground-max-turns: 3
      chain-max-depth: 4
```

## 담당 에이전트

- Implement: `backend-architect` — Orchestrator/Handler/Tracker 3계층 설계, Mode 상태기계
- Review: `architect-reviewer` — 기존 `FollowUpService` 확장 vs `ResumeInterviewOrchestrator` 분리 판단, SOLID (`InterviewTurnService` 는 실재하지 않음 — INVENTORY.md:108)
- Review: `qa` — Playground ↔ Interrogation 전환 시나리오, 엣지 케이스

## Flag Exit Criteria

`rehearse.features.resume-track` 은 **release flag**. 다음 조건 충족 시 **plan-12** 에서 제거:
- 이력서 업로드 유저 100% 롤아웃 2주 유지
- 에러율 ≤ 일반 트랙 + 0.5%p
- plan-05/06/07 전체 기능이 하나의 resume 트랙으로 단일 경로화 완료
- 제거 범위: flag 필드 + `resume_track_enabled = false` 폴백 분기 삭제 (이력서 업로드 = 항상 resume 트랙)

## 검증

1. 본인 이력서로 dogfooding 5회 — 놀이터 → 심문 → WRAP_UP 전환이 자연스러운지 체감
2. Chain 진행이 L1→L2→L3→L4 순서 (level jump/skip rate ≤ 5%)
3. Playground 모드에서 "왜", "원리" 질문 발생 0회 (정규식 필터 감지)
4. 전환 조건(claims 60% / 300자 / 종결 시그널 / playground max turns) 충족 시 자동 전환 동작
5. **WRAP_UP 전이** — `remaining_time ≤ 2분` 시 자동 모드 전환 + 새 chain 시작 차단 검증
6. **WRAP_UP 회고 질문** 생성 + 세션 자연 종료 동작
7. **Resume Exclusivity** — 위 "테스트 계약" 표 4케이스 통합 테스트 통과
8. 기존 CS/언어 면접 경로 회귀 없음 (`./gradlew test --tests "FollowUpServiceTest"`)
9. plan-10 J1 Follow-up Relevance (Resume 골든셋 20개) ≥ 4.0
10. `progress.md` 07 → Completed
