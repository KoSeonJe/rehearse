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
| `backend/src/main/java/com/rehearse/api/domain/resume/domain/ResumeMode.java` | 신규 enum (PLAYGROUND/INTERROGATION) |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` | **수정 (기존 — `InterviewTurnService` 없음)**. Interview에 `resumeSkeletonId`가 있으면 `ResumeInterviewOrchestrator`로 위임 |

## 상세

### 처리 흐름
```java
// ResumeInterviewOrchestrator.processUserTurn()
IntentResult intent = intentClassifier.classify(...);                 // plan-01 재사용
if (intent.isNotAnswer()) return handleNonAnswerIntent(...);

AnswerAnalysis analysis = answerAnalyzer.analyze(...);                 // plan-02 재사용
session.recordAnalysis(analysis);                                      // M3/plan-08용

return switch (session.getResumeMode()) {
  case PLAYGROUND -> playgroundHandler.handle(session, userUtterance, analysis);
  case INTERROGATION -> interrogationHandler.handle(session, userUtterance, analysis);
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
- Review: `architect-reviewer` — 기존 `InterviewTurnService` 확장 vs 분리 판단, SOLID
- Review: `qa` — Playground ↔ Interrogation 전환 시나리오, 엣지 케이스

## 검증

1. 본인 이력서로 dogfooding 5회 — 놀이터 → 심문 전환이 자연스러운지 체감
2. Chain 진행이 L1→L2→L3→L4 순서 (level jump/skip rate ≤ 5%)
3. Playground 모드에서 "왜", "원리" 질문 발생 0회 (정규식 필터 감지)
4. 하드 리밋(3턴) 도달 시 강제 전환 동작
5. 기존 CS/언어 면접 경로 회귀 없음 (`./gradlew test --tests "InterviewTurnServiceTest"`)
6. plan-10 J1 Follow-up Relevance (Resume 골든셋 20개) ≥ 4.0
7. `progress.md` 07 → Completed
